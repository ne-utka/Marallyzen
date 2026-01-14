generateTabCompletion:
    type: procedure
    definitions: completion_tree|args|raw_args
    script:
        - define tree <def[completion_tree]||<def[1]>||<map>>
        - define args <def[args]||<def[2]>||<list>>
        - if <[args].size||0> <= 1:
            - determine <[tree].keys||<list>>
            - stop
        - define first <[args].get[1]||null>
        - if <[first]> == null:
            - determine <[tree].keys||<list>>
            - stop
        - determine <[tree].get[<[first]>]||<list>>
