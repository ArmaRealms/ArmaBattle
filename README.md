# TitansBattle

Highly customizable battle plugin for Spigot servers

[![Spiget Downloads](https://img.shields.io/spiget/downloads/47145)](https://www.spigotmc.org/resources/47145/)
[![Discord](https://img.shields.io/discord/719557355917934613?label=discord&logo=discord)](https://discord.gg/CkNwgdE)
[![Crowdin](https://badges.crowdin.net/titansbattle/localized.svg)](https://crowdin.com/project/titansbattle)

## Winning players (PlaceholderAPI)

Use `%titansbattle_last_winner_players_<game>%` to display all players from the
latest recorded winning team for an event. Replace `<game>` with the event's
configured name, for example `%titansbattle_last_winner_players_torneio_2x2%`.
It works without a player context and can be used in tournament prize commands,
including trophy names and lore parsed through PlaceholderAPI.

Configure the separators in `config.yml`:

```yaml
placeholders:
  winner-players:
    separator: ", "
    last-separator: " e "
```

The result is `Alice e Bob` for two winners, `Alice, Bob e Carol` for three,
and just `Alice` for one. With no recorded winners, it returns an empty string.
These defaults also apply when the settings are missing from an existing config.
The list includes participating members of the winning team who died during the
tournament, and supports events with any team size. It does not list clan members
who did not participate. The existing `%titansbattle_last_winner_<game>%`
placeholder uses fixed separators `, ` and ` e `, independently of these settings.
