guard_dialog:
    type: assignment
    actions:
        on assignment:
            - trigger name:click state:true
    interact scripts:
    - guard_interact

guard_interact:
    type: interact
    steps:
        1:
            click trigger:
                script:
                    - narrate "Greetings, traveler! I am the guard of this village."
                    - wait 2s
                    - narrate "What brings you here?"
                    - wait 1s
                    - define options:<list[trade|quest|leave]>
                    - choose <[options]>:
                        - case trade:
                            - narrate "I'm afraid I have no goods to trade at the moment."
                            - narrate "Perhaps check with the merchant?"
                        - case quest:
                            - narrate "There are rumors of strange creatures in the nearby forest."
                            - narrate "If you investigate, I might have a reward for you."
                        - case leave:
                            - narrate "Safe travels, friend!"
    requirements:
        mode: all



