Option Explicit

Dim wordApp
Dim wordDoc
Dim index
Dim inputPath
Dim outputPath
Dim pageCount

If WScript.Arguments.Count < 2 Or WScript.Arguments.Count Mod 2 <> 0 Then
  WScript.Echo "Usage: cscript export-docx.vbs input.docx output.pdf [input.docx output.pdf ...]"
  WScript.Quit 2
End If

Set wordApp = CreateObject("Word.Application")
On Error Resume Next
wordApp.Visible = False
wordApp.DisplayAlerts = 0
If Err.Number <> 0 Then
  WScript.Echo "Failed to configure Word: " & Err.Description
  wordApp.Quit
  WScript.Quit 3
End If
On Error GoTo 0

For index = 0 To WScript.Arguments.Count - 1 Step 2
  inputPath = WScript.Arguments(index)
  outputPath = WScript.Arguments(index + 1)
  Set wordDoc = wordApp.Documents.Open(inputPath, False, True)
  pageCount = wordDoc.ComputeStatistics(2)
  wordDoc.ExportAsFixedFormat outputPath, 17, False, 0, 0, 0, 0, 0, True, True, 0, False, True, False
  WScript.Echo "Exported " & outputPath & " pages=" & pageCount
  wordDoc.Close 0
Next

wordApp.Quit
Set wordDoc = Nothing
Set wordApp = Nothing
