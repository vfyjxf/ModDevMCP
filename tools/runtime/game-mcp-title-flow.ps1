$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$bridgePath = Join-Path $repoRoot "Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat"
$outputRoot = Join-Path $repoRoot "build\demo\title-hover-flow"
$runStamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runDir = Join-Path $outputRoot $runStamp

if (-not (Test-Path $bridgePath)) {
    throw "Bridge launcher not found: $bridgePath"
}

New-Item -ItemType Directory -Force -Path $runDir | Out-Null

function New-InitializeRequest {
    return [ordered]@{
        jsonrpc = "2.0"
        id = 1
        method = "initialize"
        params = [ordered]@{
            protocolVersion = "2025-11-05"
            capabilities = @{}
            clientInfo = [ordered]@{
                name = "moddev-runtime-flow"
                version = "0.0.0"
            }
        }
    }
}

function New-InitializedNotification {
    return [ordered]@{
        jsonrpc = "2.0"
        method = "notifications/initialized"
    }
}

function New-ToolCallRequest {
    param(
        [int]$Id,
        [string]$Name,
        [hashtable]$Arguments
    )

    return [ordered]@{
        jsonrpc = "2.0"
        id = $Id
        method = "tools/call"
        params = [ordered]@{
            name = $Name
            arguments = $Arguments
        }
    }
}

function Invoke-GameMcpBatch {
    param(
        [array]$ToolCalls
    )

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "cmd.exe"
    $psi.Arguments = "/c `"$bridgePath`""
    $psi.WorkingDirectory = $repoRoot
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true

    $process = [System.Diagnostics.Process]::Start($psi)
    try {
        $requests = @(
            (New-InitializeRequest),
            (New-InitializedNotification)
        ) + $ToolCalls
        foreach ($request in $requests) {
            $process.StandardInput.WriteLine(($request | ConvertTo-Json -Compress -Depth 20))
        }
        $process.StandardInput.Close()
        $stdout = $process.StandardOutput.ReadToEnd()
        $stderr = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) {
            throw "Bridge exited with code $($process.ExitCode): $stderr"
        }
        $responses = @{}
        foreach ($line in ($stdout -split "`r?`n")) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }
            $response = $line | ConvertFrom-Json
            if ($null -ne $response.id) {
                $responses[[string]$response.id] = $response
            }
        }
        return $responses
    } finally {
        if (-not $process.HasExited) {
            $process.Kill()
        }
        $process.Dispose()
    }
}

function Get-StructuredContent {
    param(
        $Response
    )

    if ($Response.result.isError) {
        $message = $Response.result.content[0].text
        throw "MCP tool call failed: $message"
    }
    return $Response.result.structuredContent
}

function Copy-Capture {
    param(
        [string]$SourcePath,
        [string]$TargetFileName
    )

    $targetPath = Join-Path $runDir $TargetFileName
    Copy-Item -Force -Path $SourcePath -Destination $targetPath
    return $targetPath
}

$steps = New-Object System.Collections.Generic.List[object]

$preHoverResponses = Invoke-GameMcpBatch @(
    (New-ToolCallRequest 2 "moddev.ui_get_live_screen" @{}),
    (New-ToolCallRequest 3 "moddev.ui_capture" @{ source = "auto" })
)
$preHoverScreen = Get-StructuredContent $preHoverResponses["2"]
$preHoverCapture = Get-StructuredContent $preHoverResponses["3"]
$preHoverImage = Copy-Capture $preHoverCapture.imagePath "step-01-pre-hover.png"
$steps.Add([ordered]@{
    step = "pre-hover"
    liveScreen = $preHoverScreen
    capture = $preHoverCapture
    copiedImagePath = $preHoverImage
}) | Out-Null

$hoverX = [int][Math]::Round($preHoverScreen.guiWidth / 2.0)
$hoverY = [int]([Math]::Floor($preHoverScreen.guiHeight / 4.0) + 58)

$hoverResponses = Invoke-GameMcpBatch @(
    (New-ToolCallRequest 2 "moddev.ui_get_live_screen" @{}),
    (New-ToolCallRequest 3 "moddev.input_action" @{
        action = "hover"
        coordinateSpace = "gui"
        x = $hoverX
        y = $hoverY
        hoverDelayMs = 250
    }),
    (New-ToolCallRequest 4 "moddev.ui_capture" @{ source = "auto" }),
    (New-ToolCallRequest 5 "moddev.ui_get_interaction_state" @{})
)
$hoverScreen = Get-StructuredContent $hoverResponses["2"]
$hoverAction = Get-StructuredContent $hoverResponses["3"]
$hoverCapture = Get-StructuredContent $hoverResponses["4"]
$hoverState = Get-StructuredContent $hoverResponses["5"]
$hoverImage = Copy-Capture $hoverCapture.imagePath "step-02-post-hover.png"
$steps.Add([ordered]@{
    step = "post-hover"
    liveScreen = $hoverScreen
    action = $hoverAction
    interactionState = $hoverState
    capture = $hoverCapture
    copiedImagePath = $hoverImage
}) | Out-Null

$stepLogPath = Join-Path $runDir "step-log.json"
$steps | ConvertTo-Json -Depth 20 | Set-Content -Encoding UTF8 $stepLogPath

Write-Host "Runtime flow completed."
Write-Host "Output directory: $runDir"
Write-Host "Pre-hover image:  $preHoverImage"
Write-Host "Post-hover image: $hoverImage"
Write-Host "Step log:         $stepLogPath"
