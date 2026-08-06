$socket = New-Object System.Net.Sockets.TcpClient("localhost", 5000)
$stream = $socket.GetStream()

# Login
$login = [System.Text.Encoding]::UTF8.GetBytes("LOGIN joao 123 SESP`n")
$stream.Write($login, 0, $login.Length)
$stream.Flush()
Start-Sleep -Milliseconds 500

# Prepara o arquivo a ser enviado (ex: arq1.txt ou arquivo de teste)
$filePath = "arq1.txt"
if (Test-Path $filePath) {
    $fileContent = [System.IO.File]::ReadAllBytes($filePath)
    $fileName = [System.IO.Path]::GetFileName($filePath)
} else {
    $fileContent = [System.Text.Encoding]::UTF8.GetBytes("Conteudo do arquivo enviado via federacao!")
    $fileName = "teste.txt"
}
$fileSize = $fileContent.Length

# Envia comando FILE
$cmd = [System.Text.Encoding]::UTF8.GetBytes("FILE maria@MS $fileName $fileSize`n")
$stream.Write($cmd, 0, $cmd.Length)
$stream.Flush()

# Envia bytes binarios do arquivo
$stream.Write($fileContent, 0, $fileSize)
$stream.Flush()

Write-Host "Arquivo '$fileName' ($fileSize bytes) enviado com sucesso!"
