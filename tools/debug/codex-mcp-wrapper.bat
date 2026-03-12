@echo off
setlocal
set ROOT=%~dp0..\..
set LOG_FILE=%ROOT%\build\codex-mcp-wrapper.log
set DEBUG_LOG=%ROOT%\build\codex-mcp-handshake.log

echo [%DATE% %TIME%] start>>"%LOG_FILE%"
echo [%DATE% %TIME%] cwd=%CD%>>"%LOG_FILE%"
echo [%DATE% %TIME%] JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS%>>"%LOG_FILE%"

"C:\Program Files\Zulu\zulu-21\bin\java.exe" -Dmoddevmcp.stdio.debugLog="%DEBUG_LOG%" @"%ROOT%\Mod\build\moddevmcp\embedded-mcp\embedded-mcp-java.args"
set EXIT_CODE=%ERRORLEVEL%

echo [%DATE% %TIME%] exit=%EXIT_CODE%>>"%LOG_FILE%"
exit /b %EXIT_CODE%
