# Configuração Redis - TitansBattle

## Exemplo de Configuração

### 1. Servidor Master (onde os eventos são iniciados)

```yaml
redis:
  enabled: true
  host: "localhost"
  port: 6379
  username: ""  # para autenticação com usuário (Redis 6+)
  password: ""
  database: 0
  master: true
  target_servers:
    lobby:
      enabled: true
      worlds: []  # vazio = todos os mundos
    survival:
      enabled: true
      worlds: ["mundo1", "mundo2"]  # específicos
    creative:
      enabled: false
      worlds: []
```

### 2. Servidor Slave (recebe as mensagens)

```yaml
redis:
  enabled: true
  host: "localhost"  # mesmo IP do Redis
  port: 6379
  username: ""
  password: ""
  database: 0
  master: false
  target_servers: {}  # slaves não precisam configurar targets
```

## Tipos de Mensagens Enviadas

O sistema detecta automaticamente os seguintes tipos de evento:

- **GAME_STARTING**: Quando um evento está iniciando
- **PLAYER_JOINED**: Quando um jogador entra em um evento
- **PLAYER_LEFT**: Quando um jogador sai de um evento
- **GAME_ENDED**: Quando um evento termina
- **ANNOUNCEMENT**: Outras mensagens importantes

## Como Funciona

1. **Master Server**: Envia mensagens quando eventos acontecem
2. **Slave Servers**: Recebem e exibem as mensagens para jogadores online
3. **Filtragem**: Mensagens só são exibidas nos servidores/mundos configurados

## Benefícios

- Notificações em tempo real entre servidores
- Filtragem por servidor e mundo
- Sistema assíncrono (não bloqueia o jogo)
- Fallback graceful se Redis estiver indisponível