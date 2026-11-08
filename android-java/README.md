# Android nativo (Java) — Terço, Contemple os Mistérios v0.4.0 beta

Código Java independente do PWA e sem WebView. A compilação do GitHub Actions gera as pinturas WebP e narrações OGG localmente, preservando o conteúdo próprio do aplicativo Android e incluindo tudo dentro do APK.

- Android 6 (API 23) ou superior
- Sem permissão de internet
- 20 cenas WebP, 28 áudios pt-BR, 4 grupos de cinco mistérios
- Calendário litúrgico simplificado brasileiro, oração guiada e progresso local
- Compilação com AGP 8.9.2, Gradle 8.11.1, JDK 17 e Android SDK 35

**Importante:** APK publicado é assinado com chave debug temporária do GitHub Actions. Atualizações podem requerer desinstalar a beta, eliminando progresso e intenções locais. Uma distribuição de produção exigirá assinatura persistente, teste em Android real e revisão de acessibilidade.

## Android v0.4.1-beta — áudio compatível e testes reais

A compilação passa a converter as 28 narrações locais de OGG/Opus para MP3, compatível com versões antigas do Android. O app continua sem permissão de internet. O CI executa abertura, clique em oração, tentativa de reprodução, avanço e retomada de progresso no emulador Android 9 com Wi-Fi/dados desligados; salva capturas. A beta ainda utiliza a chave de debug temporária do runner, não sendo apropriada como atualização assinada estável.

## v0.4.2 — tela de oração e portabilidade do progresso

A pintura e a oração aparecem antes do rosário completo, agora recolhido por padrão. Os botões de avançar e voltar permanecem acessíveis na parte inferior, acima da navegação. Em Ajustes, é possível exportar e importar um backup JSON local pelo seletor de arquivos do Android, sem pedir permissão de Internet ou de armazenamento completo. O backup inclui intenções pessoais; guarde-o em local privado. Esta proteção ajuda quando a assinatura temporária da beta obriga a reinstalação, mas **não** substitui uma assinatura estável permanente. A compilação automatizada testa a presença dessas opções no emulador Android 9 offline.
