rem for all envisat products in folder c:\data\Asar 
rem run ASAR-Calibration and produce the ouput in folder c:\output

for /r C:\data\Asar %%X in (*.N1) do (gpt Calibration "%%X" -t "C:\output\%%~nX.dim")