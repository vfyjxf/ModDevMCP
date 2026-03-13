$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$bridgePath = Join-Path $repoRoot "Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat"
$outputRoot = Join-Path $repoRoot "build\demo\title-singleplayer-flow"
$runStamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runDir = Join-Path $outputRoot $runStamp
$titleScreenClass = "net.minecraft.client.gui.screens.TitleScreen"
$selectWorldScreenClass = "net.minecraft.client.gui.screens.worldselection.SelectWorldScreen"

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
                name = "moddev-title-singleplayer-flow"
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

function Wait-ForScreenChange {
    param(
        [string]$ExpectedScreenClass,
        [int]$TimeoutMs = 5000,
        [int]$PollIntervalMs = 200
    )

    $startedAt = Get-Date
    $observations = New-Object System.Collections.Generic.List[object]
    while (((Get-Date) - $startedAt).TotalMilliseconds -lt $TimeoutMs) {
        $responses = Invoke-GameMcpBatch @(
            (New-ToolCallRequest 2 "moddev.ui_get_live_screen" @{})
        )
        $screen = Get-StructuredContent $responses["2"]
        $observations.Add([ordered]@{
            observedAt = (Get-Date).ToString("o")
            screenClass = $screen.screenClass
            driverId = $screen.driverId
            active = $screen.active
        }) | Out-Null
        if ($screen.screenClass -eq $ExpectedScreenClass) {
            return [ordered]@{
                matched = $true
                screen = $screen
                observations = $observations
            }
        }
        Start-Sleep -Milliseconds $PollIntervalMs
    }
    return [ordered]@{
        matched = $false
        screen = $null
        observations = $observations
    }
}

$steps = New-Object System.Collections.Generic.List[object]

$titleResponses = Invoke-GameMcpBatch @(
    (New-ToolCallRequest 2 "moddev.ui_get_live_screen" @{}),
    (New-ToolCallRequest 3 "moddev.ui_query" @{
        selector = @{
            id = "button-singleplayer"
        }
    }),
    (New-ToolCallRequest 4 "moddev.ui_capture" @{ source = "auto" })
)
$titleScreen = Get-StructuredContent $titleResponses["2"]
if ($titleScreen.screenClass -ne $titleScreenClass) {
    throw "Expected title screen before click, but was: $($titleScreen.screenClass)"
}
$singleplayerQuery = Get-StructuredContent $titleResponses["3"]
$titleCapture = Get-StructuredContent $titleResponses["4"]
$titleImage = Copy-Capture $titleCapture.imagePath "step-01-title-screen.png"

$targets = @($singleplayerQuery.targets)
if ($targets.Count -ne 1) {
    throw "Expected exactly one button-singleplayer target, but got: $($targets.Count)"
}
$target = $targets[0]
$bounds = $target.bounds
$clickX = [int]([Math]::Floor($bounds.x + ($bounds.width / 2.0)))
$clickY = [int]([Math]::Floor($bounds.y + ($bounds.height / 2.0)))

$steps.Add([ordered]@{
    step = "title-screen"
    liveScreen = $titleScreen
    query = $singleplayerQuery
    target = $target
    clickPoint = @{
        x = $clickX
        y = $clickY
        coordinateSpace = "gui"
    }
    capture = $titleCapture
    copiedImagePath = $titleImage
}) | Out-Null

$clickResponses = Invoke-GameMcpBatch @(
    (New-ToolCallRequest 2 "moddev.input_action" @{
        action = "click"
        coordinateSpace = "gui"
        x = $clickX
        y = $clickY
        button = 0
        screenClass = $titleScreenClass
    })
)
$clickAction = Get-StructuredContent $clickResponses["2"]

$waitResult = Wait-ForScreenChange -ExpectedScreenClass $selectWorldScreenClass
if (-not $waitResult.matched) {
    throw "Timed out waiting for $selectWorldScreenClass"
}

Start-Sleep -Milliseconds 500

$selectWorldResponses = Invoke-GameMcpBatch @(
    (New-ToolCallRequest 2 "moddev.ui_get_live_screen" @{}),
    (New-ToolCallRequest 3 "moddev.ui_query" @{
        selector = @{
            id = "button-create-new-world"
        }
    }),
    (New-ToolCallRequest 4 "moddev.ui_capture" @{ source = "framebuffer" }),
    (New-ToolCallRequest 5 "moddev.ui_get_interaction_state" @{})
)
$selectWorldScreen = Get-StructuredContent $selectWorldResponses["2"]
$selectWorldQuery = Get-StructuredContent $selectWorldResponses["3"]
$selectWorldCapture = Get-StructuredContent $selectWorldResponses["4"]
$selectWorldState = Get-StructuredContent $selectWorldResponses["5"]
$selectWorldImage = Copy-Capture $selectWorldCapture.imagePath "step-02-select-world-screen.png"

$steps.Add([ordered]@{
    step = "select-world-screen"
    clickAction = $clickAction
    wait = $waitResult
    liveScreen = $selectWorldScreen
    query = $selectWorldQuery
    interactionState = $selectWorldState
    capture = $selectWorldCapture
    copiedImagePath = $selectWorldImage
}) | Out-Null

$stepLogPath = Join-Path $runDir "step-log.json"
$steps | ConvertTo-Json -Depth 20 | Set-Content -Encoding UTF8 $stepLogPath

Write-Host "Runtime flow completed."
Write-Host "Output directory: $runDir"
Write-Host "Title screen image:    $titleImage"
Write-Host "Select world image:    $selectWorldImage"
Write-Host "Step log:              $stepLogPath"
