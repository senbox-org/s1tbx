rem input parameter should be in the form "master,slave1,slave2"

gpt GCPSelectionGraph.xml -Pfilelist="%1" -Ptarget="%CD%\gcp_selected.dim" & gpt WarpGraph.xml -Pfile="%CD%\gcp_selected.dim" -Ptarget=%CD%\coregistered_stack.dim