#!/bin/sh
DISCORD_BOT_TOKEN=
BCDICE_API_URL=
IGNORE_ERROR=
# export BCDICE_PASSWORD=PleaseChangeMeIfYouUseThis
# export BCDICE_API_SECONDARY=http://secondary.bcdice-api.yourdomain.co.jp/
# export BCDICE_DEFAULT_SYSTEM=DiceBot
# export BCDICE_MENTION_MODE=1
# export BCDICE_RESULT_DISPLAY_FORMAT=V1

java -jar discord-bcdicebot.jar "$DISCORD_BOT_TOKEN" "$BCDICE_API_URL" "$IGNORE_ERROR"
