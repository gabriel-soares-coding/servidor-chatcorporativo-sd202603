$socket = New-Object System.Net.Sockets.TcpClient("localhost", 5001)
$stream = $socket.GetStream()

function Read-LineText($stream) {
    $ms = New-Object System.IO.MemoryStream
    while ($true) {
        $b = $stream.ReadByte()
        if ($b -eq -1 -or $b -eq 10) { break }
        if ($b -ne 13) { $ms.WriteByte($b) }
    }
    if ($ms.Length -eq 0 -and $b -eq -1) { return $null }
    return [System.Text.Encoding]::UTF8.GetString($ms.ToArray())
}

# Login
$login = [System.Text.Encoding]::UTF8.GetBytes("LOGIN maria 123 SESP`n")
$stream.Write($login, 0, $login.Length)
$stream.Flush()

Write-Host "Destinatario maria conectado em MS. Aguardando arquivo..."

while ($socket.Connected) {
    $line = Read-LineText $stream
    if ($null -eq $line) { break }
    Write-Host "MS Recebeu: $line"

    if ($line.StartsWith("FRECV")) {
        $parts = $line.Split(" ")
        $sender = $parts[1]
        $filename = $parts[2]
        $fileSize = [int]$parts[3]

        Write-Host "-> Baixando arquivo '$filename' ($fileSize bytes) de $sender..."

        # Le os bytes do arquivo diretamente do stream
        $buffer = New-Object byte[] $fileSize
        $totalRead = 0
        while ($totalRead -lt $fileSize) {
            $read = $stream.Read($buffer, $totalRead, $fileSize - $totalRead)
            if ($read -le 0) { break }
            $totalRead += $read
        }

        # Salva o arquivo em disco
        $outputPath = "recebido_$filename"
        [System.IO.File]::WriteAllBytes($outputPath, $buffer)
        Write-Host "SUCESSO: Arquivo salvo como '$outputPath'!"
        Write-Host "Conteudo lido do arquivo: "$([System.Text.Encoding]::UTF8.GetString($buffer))
    }
}
