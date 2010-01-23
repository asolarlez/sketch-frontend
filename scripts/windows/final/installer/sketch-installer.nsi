; gatoatigrado (nicholas tung) [ntung at ntung]
; pieced together from various web examples
; http://nsis.sourceforge.net/Java_Launcher_with_automatic_JRE_installation
; http://nsis.sourceforge.net/Path_Manipulation
; http://nsis.sourceforge.net/Docs/Modern%20UI/Readme.html
; http://nsis.sourceforge.net/TextReplace_plugin

!include MUI2.nsh
!include EnvVarUpdate.nsh
!include TextReplace.nsh

Name "SKETCH"
Caption "SKETCH ${version} Installer"
;Icon "gear.ico"
OutFile "sketch-${version}-installer-${osname}-${osarch}.exe"
InstallDir "$LOCALAPPDATA\SKETCH"
RequestExecutionLevel user

!define MUI_ABORTWARNING

;Pages

    !insertmacro MUI_PAGE_LICENSE "COPYING"
    !insertmacro MUI_PAGE_DIRECTORY
    !insertmacro MUI_PAGE_INSTFILES

    !insertmacro MUI_UNPAGE_CONFIRM
    !insertmacro MUI_UNPAGE_INSTFILES

    !insertmacro MUI_LANGUAGE "English"

section
    setOutPath "$INSTDIR"
    file sketch
    file sketch.bat
    file "sketch-${version}-all-${osname}-${osarch}.jar"
    ${EnvVarUpdate} $0 "PATH" "P" "HKCU" "$INSTDIR"

    ; Java installation
    ;GetVersion::WindowsPlatformArchitecture
    ;Pop $R0
    ;IntCmp $R0 64 Set64BitVars 0
    ;    GoTo DoneSetVars
    ;    # I could only find url's that don't expire for JDK7.
    ;    StrCpy $JRE_URL "http://www.java.net/download/jdk7/binaries/jdk-7-ea-bin-b79-windows-i586-14_jan_2010.exe"
    ;Set64BitVars:
    ;    StrCpy $JRE_URL "http://www.java.net/download/jdk7/binaries/jdk-7-ea-bin-b79-windows-x64-14_jan_2010.exe"
    ;DoneSetVars:

    ;Call GetJRE
    ;Pop $R0
    ;${textreplace::ReplaceInFile} "$INSTDIR\sketch.bat" "$INSTDIR\sketch.bat" "JAVAPROGPATH" "$R0" "/S=1" $0

    writeUninstaller "$INSTDIR\uninstaller.exe"
sectionEnd

section "Uninstall"
    delete "$INSTDIR\uninstaller.exe"
    delete "$INSTDIR\sketch"
    delete "$INSTDIR\sketch.bat"
    delete "$INSTDIR\sketch-${version}-all-${osname}-${osarch}.jar"
    ${un.EnvVarUpdate} $0 "PATH" "R" "HKCU" "$INSTDIR"
    RmDir "$INSTDIR"
sectionEnd
