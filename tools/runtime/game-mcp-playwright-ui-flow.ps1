$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$bridgePath = Join-Path $repoRoot "Mod\build\moddevmcp\game-mcp\run-game-mcp-bridge.bat"
$outputRoot = Join-Path $repoRoot "build\demo\playwright-ui-flow"
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
                name = "moddev-playwright-ui-flow"
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
            $process.StandardInput.WriteLine(($request | ConvertTo-Json -Compress -Depth 30))
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

function Wait-ForScreenClass {
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
    (New-ToolCallRequest 3 "moddev.ui_session_open" @{})
)
$titleScreen = Get-StructuredContent $titleResponses["2"]
if ($titleScreen.screenClass -ne $titleScreenClass) {
    throw "Expected title screen before automation flow, but was: $($titleScreen.screenClass)"
}
$titleSession = Get-StructuredContent $titleResponses["3"]
$singleplayerRef = @($titleSession.refs | Where-Object { $_.targetId -eq "button-singleplayer" }) | Select-Object -First 1
if ($null -eq $singleplayerRef) {
    throw "Could not find button-singleplayer in ui_session_open refs"
}

$titleBatchResponses = Invoke-GameMcpBatch @(
    (New-ToolCallRequest 2 "moddev.ui_batch" @{
        sessionId = $titleSession.sessionId
        stopOnError = $true
        steps = @(
            @{
                type = "hoverRef"
                refId = $singleplayerRef.refId
            },
            @{
                type = "screenshot"
                refId = $singleplayerRef.refId
                source = "auto"
            },
            @{
                type = "clickRef"
                refId = $singleplayerRef.refId
            }
        )
    }),
    (New-ToolCallRequest 3 "moddev.ui_trace_get" @{
        sessionId = $titleSession.sessionId
    })
)
$titleBatch = Get-StructuredContent $titleBatchResponses["2"]
$titleTrace = Get-StructuredContent $titleBatchResponses["3"]
if (-not $titleBatch.success) {
    throw "Title batch failed at step $($titleBatch.failureStepIndex)"
}
@($titleBatch.steps) | ForEach-Object {
    if ($_.type -eq "screenshot") {
        $titleCheckpointImage = Copy-Capture $_.result.imagePath "step-01-title-singleplayer-button.png"
    }
}
if (-not $titleCheckpointImage) {
    throw "Title batch did not produce screenshot output"
}

$steps.Add([ordered]@{
    step = "title-screen-batch"
    liveScreen = $titleScreen
    session = $titleSession
    selectedRef = $singleplayerRef
    batch = $titleBatch
    trace = $titleTrace
    copiedImagePath = $titleCheckpointImage
}) | Out-Null

$waitResult = Wait-ForScreenClass -ExpectedScreenClass $selectWorldScreenClass
if (-not $waitResult.matched) {
    throw "Timed out waiting for $selectWorldScreenClass"
}

Start-Sleep -Milliseconds 500

$worldResponses = Invoke-GameMcpBatch @(
    (New-ToolCallRequest 2 "moddev.ui_get_live_screen" @{}),
    (New-ToolCallRequest 3 "moddev.ui_session_open" @{})
)
$worldScreen = Get-StructuredContent $worldResponses["2"]
$worldSession = Get-StructuredContent $worldResponses["3"]
$worldScreenshotResponses = Invoke-GameMcpBatch @(
    (New-ToolCallRequest 2 "moddev.ui_screenshot" @{
        sessionId = $worldSession.sessionId
        source = "framebuffer"
    }),
    (New-ToolCallRequest 3 "moddev.ui_trace_get" @{
        sessionId = $worldSession.sessionId
    })
)
$worldScreenshot = Get-StructuredContent $worldScreenshotResponses["2"]
$worldTrace = Get-StructuredContent $worldScreenshotResponses["3"]
$worldImage = Copy-Capture $worldScreenshot.imagePath "step-02-select-world-screen.png"

$steps.Add([ordered]@{
    step = "select-world-screen"
    wait = $waitResult
    liveScreen = $worldScreen
    session = $worldSession
    screenshot = $worldScreenshot
    trace = $worldTrace
    copiedImagePath = $worldImage
}) | Out-Null

$stepLogPath = Join-Path $runDir "step-log.json"
$steps | ConvertTo-Json -Depth 30 | Set-Content -Encoding UTF8 $stepLogPath

Write-Host "Playwright-style UI flow completed."
Write-Host "Output directory:      $runDir"
Write-Host "Title checkpoint:      $titleCheckpointImage"
Write-Host "Select world image:    $worldImage"
Write-Host "Step log:              $stepLogPath"
