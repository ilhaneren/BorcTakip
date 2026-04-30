# BorcTakip

Kişisel borç takip uygulaması. Sana borçlu olanları ve senin borçlu olduklarını kolayca kayıt altına alır; sesli komutlarla hızlıca borç ekleyebilirsin.

## Özellikler

- Borç ekleme, düzenleme ve silme
- Kime borçlusun / kimler sana borçlu ayrımı
- Sesli komut ile borç girişi (Vosk offline ses tanıma)
- Room veritabanı ile yerel depolama
- Karanlık/aydınlık tema desteği

## Teknolojiler

- **Kotlin** + Android SDK
- **Room** — yerel veritabanı (SQLite üstü ORM)
- **Vosk** — offline ses tanıma (internet gerektirmez)
- **View Binding** — layout bağlama
- **KSP** — Kotlin Symbol Processing (Room için)

## Gereksinimler

- Android 7.0 (API 24) ve üzeri
- Android Studio Hedgehog veya daha yenisi

## Kurulum

1. Repoyu klonla
2. Android Studio ile aç
3. Vosk modelini `app/src/main/assets/model/` dizinine koy (bkz. [Vosk modelleri](https://alphacephei.com/vosk/models))
4. Çalıştır
