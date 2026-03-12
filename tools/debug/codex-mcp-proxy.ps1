param(
    [Parameter(Mandatory = $true)]
    [string]$ServerCommand,

    [string]$ServerArgsJson = '[]',

    [Parameter(Mandatory = $true)]
    [string]$LogDir
)

$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$stdinLog = Join-Path $LogDir 'stdin.log'
$stdoutLog = Join-Path $LogDir 'stdout.log'
$stderrLog = Join-Path $LogDir 'stderr.log'
$metaLog = Join-Path $LogDir 'meta.log'

function Write-Meta([string]$message) {
    Add-Content -Path $metaLog -Value ("[{0}] {1}" -f (Get-Date).ToString("o"), $message)
}

$serverInfo = New-Object System.Diagnostics.ProcessStartInfo
$serverInfo.FileName = $ServerCommand
$serverArgs = @()
if ($ServerArgsJson -and $ServerArgsJson.Trim().Length -gt 0) {
    $serverArgs = ConvertFrom-Json -InputObject $ServerArgsJson
}
foreach ($arg in $serverArgs) {
    $null = $serverInfo.ArgumentList.Add($arg)
}
$serverInfo.UseShellExecute = $false
$serverInfo.RedirectStandardInput = $true
$serverInfo.RedirectStandardOutput = $true
$serverInfo.RedirectStandardError = $true
$serverInfo.CreateNoWindow = $true

$server = [System.Diagnostics.Process]::Start($serverInfo)
Write-Meta ("started pid={0} file={1} args={2}" -f $server.Id, $ServerCommand, ($serverArgs -join ' '))

$copyBlock = {
    param($inputStream, $outputStream, [string]$logPath, [string]$direction)
    Write-Meta ("copy-start direction={0}" -f $direction)
    $buffer = New-Object byte[] 4096
    while ($true) {
        $read = $inputStream.Read($buffer, 0, $buffer.Length)
        if ($read -le 0) {
            Write-Meta ("copy-eof direction={0}" -f $direction)
            break
        }
        Write-Meta ("copy-bytes direction={0} count={1}" -f $direction, $read)
        [System.IO.File]::Open($logPath, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite).Dispose()
        $file = [System.IO.File]::Open($logPath, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
        try {
            $file.Write($buffer, 0, $read)
        } finally {
            $file.Dispose()
        }
        $outputStream.Write($buffer, 0, $read)
        $outputStream.Flush()
    }
}

$stdinTask = [System.Threading.Tasks.Task]::Run([Action]{
    try {
        Write-Meta "stdin-task-start"
        & $copyBlock ([Console]::OpenStandardInput()) $server.StandardInput.BaseStream $stdinLog 'stdin'
        $server.StandardInput.Close()
        Write-Meta "stdin-task-closed-server-stdin"
    } catch {
        Add-Content -Path $metaLog -Value ("[{0}] stdin copy failed: {1}" -f (Get-Date).ToString("o"), $_)
    }
})

$stdoutTask = [System.Threading.Tasks.Task]::Run([Action]{
    try {
        Write-Meta "stdout-task-start"
        & $copyBlock $server.StandardOutput.BaseStream ([Console]::OpenStandardOutput()) $stdoutLog 'stdout'
    } catch {
        Add-Content -Path $metaLog -Value ("[{0}] stdout copy failed: {1}" -f (Get-Date).ToString("o"), $_)
    }
})

$stderrTask = [System.Threading.Tasks.Task]::Run([Action]{
    try {
        Write-Meta "stderr-task-start"
        $stderr = $server.StandardError.ReadToEnd()
        if ($stderr.Length -gt 0) {
            Set-Content -Path $stderrLog -Value $stderr -Encoding utf8NoBOM
            Write-Meta ("stderr-bytes count={0}" -f $stderr.Length)
        }
        Write-Meta "stderr-task-eof"
    } catch {
        Add-Content -Path $metaLog -Value ("[{0}] stderr copy failed: {1}" -f (Get-Date).ToString("o"), $_)
    }
})

$server.WaitForExit()
[System.Threading.Tasks.Task]::WaitAll(@($stdinTask, $stdoutTask, $stderrTask))
Write-Meta ("exited code={0}" -f $server.ExitCode)
exit $server.ExitCode
