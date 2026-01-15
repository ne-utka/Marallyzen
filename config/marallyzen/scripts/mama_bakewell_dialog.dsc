mama_bakewell_dialog:
    type: assignment
    actions:
        on assignment:
            - trigger name:click state:true
    interact scripts:
    - mama_bakewell_interact

mama_bakewell_interact:
    type: interact
    steps:
        1:
            click trigger:
                script:
                    - narrate "Ох, милый, ты заглянул как раз вовремя."
                    - play_audio file:"dialogues/mama_bakewell_01.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Ох, милый, ты заглянул как раз вовремя."
                    - narrate "Тут всё пахнет корицей и ванилью."
                    - play_audio file:"dialogues/mama_bakewell_02.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Тут всё пахнет корицей и ванилью."
                    - narrate "В моей печи сейчас томится пирог."
                    - play_audio file:"dialogues/mama_bakewell_03.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"В моей печи сейчас томится пирог."
                    - narrate "Если есть минутка, поговорим?"
                    - play_audio file:"dialogues/mama_bakewell_04.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Если есть минутка, поговорим?"
                    - define options:<list[о_ней|рецепт|новости|прощание]>
                    - define expressions:<list[idle|laught|idle|wave]>
                    - choose <[options]>:
                        - case о_ней:
                            - narrate "Я Мама Бейквелл. Пеку сладости для всей округи."
                            - play_audio file:"dialogues/mama_bakewell_05.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Я Мама Бейквелл. Пеку сладости для всей округи."
                            - narrate "Когда-то у меня была лавка на площади, но я перебралась сюда — тише и спокойнее."
                            - play_audio file:"dialogues/mama_bakewell_06.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Когда-то у меня была лавка на площади, но я перебралась сюда — тише и спокойнее."
                        - case рецепт:
                            - narrate "Секрет простой: тёплые руки и доброе слово."
                            - play_audio file:"dialogues/mama_bakewell_07.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Секрет простой: тёплые руки и доброе слово."
                            - narrate "Сначала мука и масло, потом мёд, и уже в конце — немного лимонной цедры."
                            - play_audio file:"dialogues/mama_bakewell_08.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Сначала мука и масло, потом мёд, и уже в конце — немного лимонной цедры."
                            - narrate "Но главный ингредиент — терпение."
                            - play_audio file:"dialogues/mama_bakewell_09.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Но главный ингредиент — терпение."
                        - case новости:
                            - narrate "Люди шепчутся, что в лесу снова видели огни."
                            - play_audio file:"dialogues/mama_bakewell_10.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Люди шепчутся, что в лесу снова видели огни."
                            - narrate "Если пойдёшь туда, не забудь фонарь и тёплый плащ."
                            - play_audio file:"dialogues/mama_bakewell_11.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Если пойдёшь туда, не забудь фонарь и тёплый плащ."
                            - narrate "И не трогай старые колодцы, слышишь?"
                            - play_audio file:"dialogues/mama_bakewell_12.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"И не трогай старые колодцы, слышишь?"
                        - case прощание:
                            - narrate "Возвращайся, когда проголодаешься."
                            - play_audio file:"dialogues/mama_bakewell_13.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"Возвращайся, когда проголодаешься."
                            - narrate "И береги себя, милый."
                            - play_audio file:"dialogues/mama_bakewell_14.ogg.wav" source:npc mode:positional radius:10 blocking:false narration:"И береги себя, милый."
    requirements:
        mode: all
