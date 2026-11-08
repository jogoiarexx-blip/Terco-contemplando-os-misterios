# Terço — Contemple os Mistérios

Aplicativo católico para rezar o Santo Terço e contemplar os mistérios da vida de Jesus e Maria.

## Versões

- **PWA (`web/`)**: versão de navegador instalável pelo GitHub Pages, com cache offline após o primeiro acesso completo.
- **Android Java (`android-java/`)**: aplicativo nativo Java, sem WebView, para uso offline após instalação do APK.

## V0.3

Planejamento: 20 mistérios com cenas e reflexões, 28 áudios narrados em português, calendário litúrgico brasileiro, progresso salvo e opção de contemplação silenciosa.

O site instalável e o APK são tecnologias distintas: instalar pelo navegador não equivale a instalar o aplicativo Java nativo. A versão Android exige a compilação de um APK. O código-fonte e os pacotes completos estão em preparação para publicação aqui.

## Imagens WebP (v0.3.1)

O aplicativo e o cache offline usam agora `web/assets/misterios/{gozosos,luminosos,dolorosos,gloriosos}-{1..5}.webp`. A publicação preserva arquivos WebP já enviados ao repositório e gera ilustrações de reserva quando estiverem ausentes. Para utilizar as pinturas detalhadas do pacote, enviar os 20 WebP para essa pasta e executar o workflow novamente.

## Pinturas publicadas — v0.3.2

As 20 pinturas WebP enviadas foram movidas para `web/assets/misterios/`, e as telas de oração e contemplação usam esses arquivos. O Service Worker renova o cache offline na versão `terco-contemple-v0.3.2`. O Android Java permanece em `android-java/` para distribuição posterior.
