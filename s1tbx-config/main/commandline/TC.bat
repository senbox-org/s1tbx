rem for all envisat products in folder WSM_Europe
rem run Terrain Correction and produce the ouput in folder c:\data\WSM_Ortho

for /r C:\data\WSM_Europe %%X in (*.dim) do (gpt TC_Graph.xml -Pfile="%%X"  -Ptarget="C:\data\WSM_Ortho\%%~nX.dim")