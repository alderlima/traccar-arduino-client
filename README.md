# Traccar Arduino Client

Um aplicativo Android completo que funciona como cliente Traccar com protocolo GT06, integra controle de hardware via Arduino através de USB OTG, e permite rastreamento em tempo real com controle remoto de motor.

## Funcionalidades

- **Rastreamento GPS em Tempo Real**: Envia localização contínua para o servidor Traccar
- **Protocolo GT06**: Implementação nativa do protocolo binário GT06
- **Controle Arduino via USB OTG**: Comunicação serial com Arduino para acionamento de relé
- **Simulação de Ignição**: Controle manual de ligar/desligar rastreamento
- **Corte de Motor**: Ativar/desativar corte do motor via relé Arduino
- **Comandos Remotos**: Recebe comandos do servidor Traccar (`engine stop`/`engine resume`)
- **Serviço em Segundo Plano**: Rastreamento contínuo mesmo com app fechado
- **Inicialização Automática**: Inicia rastreamento ao ligar o dispositivo

## Requisitos

- Android 7.0+ (API 24)
- Arduino com relé conectado ao pino 13
- Servidor Traccar em produção (smartatendimentos.shop)
- Cabo USB OTG para conectar Arduino ao celular

## Instalação

### Opção 1: Usar APK Pré-compilado

Baixe o arquivo `app-release.apk` da seção [Releases](https://github.com/seu-usuario/traccar_arduino_client/releases) e instale no seu dispositivo Android.

### Opção 2: Compilar do Código-Fonte

#### Pré-requisitos
- Android Studio ou Android SDK Command-line Tools
- JDK 11+
- Gradle 8.0+

#### Passos

1. Clone o repositório:
```bash
git clone https://github.com/seu-usuario/traccar_arduino_client.git
cd traccar_arduino_client
```

2. Abra no Android Studio ou compile via linha de comando:
```bash
./gradlew assembleRelease
```

3. O APK será gerado em `app/build/outputs/apk/release/app-release.apk`

## Configuração

### 1. Configurar Arduino

Carregue este sketch no seu Arduino:

```cpp
const int relePin = 13;

void setup() {
  Serial.begin(9600);
  pinMode(relePin, OUTPUT);
  digitalWrite(relePin, LOW);
}

void loop() {
  if (Serial.available() > 0) {
    char comando = Serial.read();
    if (comando == '1') {
      digitalWrite(relePin, HIGH);
    }
    else if (comando == '0') {
      digitalWrite(relePin, LOW);
    }
  }
}
```

### 2. Configurar Aplicativo

1. Abra o aplicativo no seu celular
2. Clique em "Configurações"
3. Insira:
   - **Endereço do Servidor**: `smartatendimentos.shop`
   - **ID do Dispositivo**: O ID que você cadastrou no Traccar
4. Clique em "Salvar"

### 3. Conectar Arduino

1. Conecte o Arduino ao celular usando cabo USB OTG
2. O aplicativo detectará automaticamente a conexão
3. O status do Arduino mudará para "Conectado"

## Uso

### Iniciar Rastreamento

1. Clique no toggle "Ignição Ligada"
2. O aplicativo conectará ao servidor Traccar
3. O status mudará para "Conectado"
4. Localização será enviada a cada 5 segundos

### Controlar Corte de Motor

- **Ativar Corte**: Clique em "Ativar Corte" para ligar o relé
- **Desativar Corte**: Clique em "Desativar Corte" para desligar o relé

### Receber Comandos do Traccar

O servidor Traccar pode enviar comandos remotos:
- `engine stop`: Ativa o corte do motor
- `engine resume`: Desativa o corte do motor

## Estrutura do Projeto

```
traccar_arduino_client/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/smartatendimentos/traccar/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── SettingsActivity.kt
│   │   │   │   ├── ArduinoCommunication.kt
│   │   │   │   ├── GT06Protocol.kt
│   │   │   │   ├── service/
│   │   │   │   │   └── TraccarService.kt
│   │   │   │   └── receiver/
│   │   │   │       └── BootReceiver.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── xml/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Dependências

- `androidx.core:core-ktx` - Android Core
- `androidx.appcompat:appcompat` - AppCompat
- `com.google.android.material:material` - Material Design
- `androidx.lifecycle:lifecycle-runtime-ktx` - Lifecycle
- `com.google.android.gms:play-services-location` - Google Play Services
- `com.github.mik3y:usb-serial-for-android` - USB Serial Communication
- `com.squareup.okhttp3:okhttp` - HTTP Client
- `com.google.code.gson:gson` - JSON
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` - Coroutines

## Permissões Necessárias

O aplicativo solicita as seguintes permissões:

- `ACCESS_FINE_LOCATION` - Localização GPS precisa
- `ACCESS_COARSE_LOCATION` - Localização aproximada
- `ACCESS_BACKGROUND_LOCATION` - Localização em segundo plano
- `INTERNET` - Comunicação com servidor
- `USB_PERMISSION` - Acesso ao dispositivo USB (Arduino)
- `WAKE_LOCK` - Manter dispositivo ativo
- `RECEIVE_BOOT_COMPLETED` - Iniciar ao ligar

## Troubleshooting

### Arduino não conecta
- Verifique se o cabo USB OTG está funcionando
- Certifique-se de que o Arduino está com o sketch correto carregado
- Reinicie o aplicativo

### Não conecta ao Traccar
- Verifique a configuração do servidor (deve ser `smartatendimentos.shop`)
- Certifique-se de que o ID do dispositivo está correto
- Verifique a conexão de internet

### GPS não funciona
- Ative o GPS do dispositivo
- Aguarde alguns segundos para o GPS adquirir sinal
- Verifique se as permissões foram concedidas

## Logs

Os logs da aplicação podem ser visualizados via:

```bash
adb logcat | grep "Traccar\|Arduino\|GT06"
```

## Contribuindo

Contribuições são bem-vindas! Por favor:

1. Faça um Fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## Licença

Este projeto está licenciado sob a MIT License - veja o arquivo LICENSE para detalhes.

## Suporte

Para suporte, abra uma issue no repositório GitHub ou entre em contato através de smartatendimentos.shop.

## Changelog

### v1.0.0 (2026-01-31)
- Release inicial
- Suporte a protocolo GT06
- Comunicação com Arduino via USB OTG
- Rastreamento em tempo real
- Controle remoto de motor
- Serviço em segundo plano
- Inicialização automática ao ligar

## Roadmap

- [ ] Suporte a múltiplos protocolos (JT808, JT/T 808, etc)
- [ ] Interface de mapa com localização em tempo real
- [ ] Histórico de viagens
- [ ] Alertas de velocidade
- [ ] Modo offline com sincronização posterior
- [ ] Suporte a Bluetooth para Arduino
- [ ] Dashboard web para monitoramento
