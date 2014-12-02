rem for all ERS products in CEOS format in folder c:\data\ERS and all sub folders
rem run ERS-Calibration and produce the ouput in folder c:\output

for /r C:\data\ERS %%X in (VDF*) do (gpt Calibration "%%X" -t "C:\output\%%~nX.dim")