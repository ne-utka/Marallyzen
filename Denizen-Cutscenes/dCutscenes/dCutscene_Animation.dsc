#==================== Denizen Cutscenes Animation =====================

## Camera Entity ################
dcutscene_camera_entity:
    type: entity
    debug: false
    entity_type: armor_stand
    mechanisms:
        marker: false
        tracking_range: 256
        visible: false
        is_small: false
        invulnerable: true
        gravity: false
        persistent: true

#========= Cutscene Animator Events ============
dcutscene_animation_events:
    type: world
    debug: false
    events:
      on player quits flagged:dcutscene_played_scene:
      - run dcutscene_animation_stop def.player:<player>
      on player exits armor_stand flagged:dcutscene_camera:
      - determine cancelled
      on player right clicks entity flagged:dcutscene_camera:
      - determine cancelled
      on player breaks block flagged:dcutscene_camera:
      - determine cancelled
      on player damages entity flagged:dcutscene_camera:
      - determine 0.0
      on player damaged flagged:dcutscene_camera:
      - determine 0.0

#========= Cutscene Animator Tasks and Procedures =========
# Start the cutscene
dcutscene_animation_begin:
    type: task
    debug: false
    definitions: scene|player|timespot|origin|world
    script:
    - define player <[player]||<player>>
    - define cutscene <server.flag[dcutscenes.<[scene]>]||null>
    - if <[cutscene]> == null:
      - debug error "Cutscene could not be found."
    - else:
      # Check if another cutscene is in progress if so stop it from playing
      - if <[player].has_flag[dcutscene_played_scene]>:
        - ~run dcutscene_animation_stop def.player:<[player]>
      # Allows the animation to begin at a certain tick
      - define timespot <duration[<[timespot].if_null[0t]>].in_ticks||0>
      # Cutscene Information
      - define settings <[cutscene.settings]>
      - define keyframes <[cutscene.keyframes]||null>
      - define length <[cutscene.length]||null>
      - if <[keyframes]> == null:
        - debug error "No keyframes found for scene <[scene]>"
        - stop
      - else if <[length]> == null:
        - debug error "Length of cutscene could not be found for scene <[scene]>"
        - stop
      #=Cutscene Setup
      # Scene UUID
      - define scene_uuid <util.random_uuid>
      - definemap scene_data name:<[cutscene.name]> uuid:<[scene_uuid]>
      - flag <[player]> dcutscene_played_scene:<[scene_data]>
      # World
      - define world <world[<[world]||<[cutscene.world].first>>]||null>
      - if <[world]> == null:
        - debug error "Invalid world for cutscene <[scene]>"
        - stop
      # Inventory
      - flag <[player]> dcutscene_played_scene_inv:<[player].inventory.map_slots>
      - inventory clear
      # Cutscene black bars
      - if <[settings.bars]>:
        - run dcutscene_bars def.player:<[player]>
      # Hide entities
      - if <[settings.hide_players]>:
        - run dcutscene_hide_players_task def.player:<[player]> def.world:<[world]>
      # Camera bound
      - define bound <[settings.camera_bound]>
      # Origin point
      - if !<[origin].exists>:
        - define origin <[settings.origin]||false>
      # Models in keyframes
      - define models <[keyframes.models]||null>
      # Elements in keyframe
      - define elements <[keyframes.elements]||null>
      # Play another scene in keyframe
      - define play_scene <[keyframes.play_scene]||null>
      # Stop point in keyframe
      - define stop_point <[keyframes.stop]||null>
      # Camera in keyframes (keys check for extra validation)
      - define camera <[keyframes.camera]||null>
      - if <[camera].keys.first.exists>:
        - define cam_count 0
        - ~run dcutscene_camera_spawn def.player:<[player]> def.timespot:<[timespot]> def.camera:<[camera]> def.bound:<[bound]> def.world:<[world]> def.origin:<[origin]> save:camera_spawn
        - define camera_ent <entry[camera_spawn].created_queue.determination.first>

      #======== Animator =========
      - repeat <duration[<[length]>].in_ticks.sub[<[timespot]>]> as:tick:
        - if !<[player].is_online> || <[player].flag[dcutscene_played_scene.uuid]||null> != <[scene_uuid]>:
          - stop
        # Tick added by timespot
        - define tick <[tick].add[<[timespot]>]>
        - flag <[player]> dcutscene_timespot:<[tick]>
        #======== Camera =========
        - if <[camera_ent].exists> && <[camera_ent].if_null[null].is_spawned||false>:
            - define cam_data <[camera.<[tick]>]||null>
            - if <[cam_data]> != null && <[cam_count]> < 1:
              - define cam_count 1
              - run dcutscene_path_move def.cutscene:<[cutscene.name]> def.timespot:<[timespot]> def.scene_uuid:<[scene_uuid]> def.entity:<[camera_ent]> def.type:camera def.world:<[world]> def.origin:<[origin]>
        #======= Models =======
        - if <[models.<[tick]>].exists>:
          - define model <[models.<[tick]>]>
          - define model_list <[model.model_list]>
          - foreach <[model_list]> as:model_uuid:
            - define model_id <[model.<[model_uuid]>.id]>
            # If the model is already spawned skip
            - define spawned_check <player.flag[dcutscene_spawned_models.<[model_id]>]||null>
            - if <[spawned_check]> != null:
              - foreach next
            - define model_data <[model.<[model_uuid]>]||null>
            - define root <[model_data.root]||none>
            # If there is a root model use the root data
            - if <[root]> != none:
              - define model_data <[models.<[root.tick]>.<[root.uuid]>]||null>
              - define path_tick <[root.tick]>
              - define path_uuid <[root.uuid]>
            - else:
              - define path_tick <[tick]>
              - define path_uuid <[model_uuid]>
            # If the model is a root model spawn it
            - if <[model_data]> != null:
              - define model_id <[model_data.id]>
              - define type <[model_data.type]>
              - define path <[model_data.path]>
              - define spawn_loc <[path.<[tick]>.location].with_world[<[world]>]>
              - choose <[type]>:
                #=Model
                - case model:
                  - define script <script[dmodels_spawn_model]||null>
                  - define model_name <[model_data.model]>
                  - if <[script]> == null:
                    - debug error "Could not spawn model <[model_name]> is your DModels up to date?"
                    - foreach next
                  - define defs <list[<[model_name]>|<[spawn_loc]>|256|<[player]>]>
                #=Player Model
                - case player_model:
                  - define script <script[pmodels_spawn_model]||null>
                  - if <[script]> == null:
                    - debug error "Could not spawn player model is it up to date?"
                    - foreach next
                  #If there is a root model use the root model skin
                  - if <[root]> != none:
                    - define skin <[path.<[root.tick]>.skin]||null>
                  - else:
                    - define skin <[path.<[tick]>.skin]||null>
                  - if <[skin]> == null:
                    - debug error "Could not determine skin for player model in dcutscene_animation_begin"
                    - foreach next
                  - if <[skin]> == none || <[skin]> == player:
                    - define skin <[player]>
                  - else:
                    - define skin <[skin].parsed||null>
                    - if <[skin]> == null:
                      - debug error "Invalid player for skin in dcutscene_animation_begin"
                      - foreach next
                    - else if !<[skin].is_player||false> && !<[skin].is_npc||false>:
                      - debug error "Invalid player for skin in dcutscene_animation_begin"
                      - foreach next
                  - define defs <list[<[spawn_loc]>|<[skin]>|<[player]>]>
                - default:
                  - foreach next
              - run <[script]> def:<[defs]> save:spawned
              - define root <entry[spawned].created_queue.determination.first>
              - chunkload <[root].location.chunk> duration:5t
              # Track spawned model
              - flag <[player]> dcutscene_spawned_models.<[model_id]>.root:<[root]>
              - flag <[player]> dcutscene_spawned_models.<[model_id]>.type:<[type]>
              - definemap path_data tick:<[path_tick]> uuid:<[path_uuid]>
              - run dcutscene_path_move def.cutscene:<[cutscene.name]> def.timespot:<[timespot]> def.scene_uuid:<[scene_uuid]> def.entity:<[root]> def.type:<[type]> def.data:<[path_data]> def.world:<[world]> def.origin:<[origin]>
        #===== Regular Animators =====
        - if <[elements]> != null:
          #=Run task check
          - define run_tasks <[elements.run_task.<[tick]>.run_task_list]||null>
          - if <[run_tasks]> != null:
            - foreach <[run_tasks]> as:task_id:
              - define data <[elements.run_task.<[tick]>.<[task_id]>]||null>
              - if <[data]> != null:
                - run dcutscene_run_task_animator_task def:<[data]>
          #=Fake Block Check
          - define fake_blocks <[elements.fake_object.fake_block.<[tick]>.fake_blocks]||null>
          - if <[fake_blocks]> != null:
            - foreach <[fake_blocks]> as:object_id:
              - define b_data <[elements.fake_object.fake_block.<[tick]>.<[object_id]>]||null>
              - if <[b_data]> != null:
                - run dcutscene_fake_block_animator def.loc:<[b_data.loc]> def.data:<[b_data]> def.world:<[world]> def.origin:<[origin]>
          #=Fake Schem Check
          - define fake_schems <[elements.fake_object.fake_schem.<[tick]>.fake_schems]||null>
          - if <[fake_schems]> != null:
            - foreach <[fake_schems]> as:object_id:
              - define s_data <[elements.fake_object.fake_schem.<[tick]>.<[object_id]>]||null>
              - if <[s_data]> != null:
                - run dcutscene_fake_schem_animator def.loc:<[s_data.loc]> def.data:<[s_data]> def.world:<[world]> def.origin:<[origin]>
          #=Particle Check
          - define particles <[elements.particle.<[tick]>.particle_list]||null>
          - if <[particles]> != null:
            - foreach <[particles]> as:particle_id:
              - define p_data <[elements.particle.<[tick]>.<[particle_id]>]||null>
              - if <[p_data]> != null:
                - run dcutscene_particle_animator_play def.player:<[player]> def.p_data:<[p_data]> def.world:<[world]> def.scene_uuid:<[scene_uuid]> def.origin:<[origin]>
          #=Sound check
          - define sounds <[elements.sound.<[tick]>.sounds]||null>
          - if <[sounds]> != null:
            - foreach <[sounds]> as:uuid:
              - define data <[elements.sound.<[tick]>.<[uuid]>]||null>
              - if <[data]> != null:
                - run dcutscene_animator_sound_play def.s_data:<[data]> def.world:<[world]> def.origin:<[origin]>
          #=Screeneffect check
          - define screeneffect <[elements.screeneffect.<[tick]>]||null>
          - if <[screeneffect]> != null:
            - run dcutscene_screeneffect def.player:<[player]> def.fade_in:<duration[<[screeneffect.fade_in]>]> def.stay:<[screeneffect.stay]> def.fade_out:<[screeneffect.fade_out]> def.color:<[screeneffect.color]>
          #=Title check
          - define title <[elements.title.<[tick]>]||null>
          - if <[title]> != null:
            - title title:<[title.title].parse_color> subtitle:<[title.subtitle].parse_color> fade_in:<[title.fade_in]> stay:<[title.stay]> fade_out:<[title.fade_out]> targets:<[player]>
          #=Command check
          - define commands <[elements.command.<[tick]>.command_list]||null>
          - if <[commands]> != null:
            - foreach <[commands]> as:uuid:
              - define data <[elements.command.<[tick]>.<[uuid]>]||null>
              - if <[data]> != null:
                - run dcutscene_command_animator def.c_data:<[data]>
          #=Message check
          - define messages <[elements.message.<[tick]>.message_list]||null>
          - if <[messages]> != null:
            - foreach <[messages]> as:uuid:
              - define message <[elements.message.<[tick]>.<[uuid]>.message].parsed||null>
              - if <[message]> != null:
                - narrate <[message].parse_color||<[message]>>
          #=Time check
          - define time <[elements.time.<[tick]>]||null>
          - if <[time]> != null:
            - run dcutscene_time_animator def.t_data:<[time]>
          #=Weather check
          - define weather_data <[elements.weather.<[tick]>]||null>
          - if <[weather_data]> != null:
            - weather player <[weather_data.weather]> reset:<[weather_data.duration]>
        #===== Play Scene Check =====
        - if <[play_scene]> != null:
          - if <[play_scene.tick].equals[<[tick]>]>:
            - run dcutscene_animation_begin def.scene:<[play_scene.cutscene]> def.player:<[player]> def.timespot:0s
            - stop
        #===== Stop Check ======
        - if <[stop_point]> != null:
          - if <[stop_point.tick].equals[<[tick]>]>:
            - run dcutscene_animation_stop def.player:<[player]>
            - stop
        - wait 1t

# Camera Spawner
dcutscene_camera_spawn:
    type: task
    debug: false
    definitions: player|timespot|camera|bound|world|origin
    script:
    - cast INVISIBILITY d:10000000000000s <[player]> hide_particles no_ambient no_icon
    - define first <[camera].keys.filter[is_more_than_or_equal_to[<[timespot]>]].first>
    - if <[origin].is_truthy>:
      - define first_loc <[origin].add[<[camera.<[first]>.origin_offset]||0,0,0>]||null>
    - else:
      - define first_loc <location[<[camera.<[first]>.location]>].with_world[<[world]>]||null>
    - if <[first_loc]> != null && <[bound]>:
      # Determine if there should be a delay to allow chunks to load
      - teleport <[player]> <[first_loc]>
      - if <[player].location.distance[<[first_loc]>]> > <[player].world.simulation_distance.mul[6]> || <[player].world> != <[world]>:
        - wait <duration[<script[dcutscenes_config].data_key[config].get[cutscene_chunk_load_delay]>].in_ticks||10.sub[1]>t
    - flag <player> dcutscene_previous_gamemode:<player.gamemode>
    - define uuid <util.random_uuid>
    - spawn dcutscene_camera_entity <[player].location> save:<[uuid]>
    - define camera_ent <entry[<[uuid]>].spawned_entity>
    - wait 2t
    - adjust <[player]> spectate:<[camera_ent]>
    - flag <[player]> dcutscene_camera:<[camera_ent]>
    #Used as mount
    - define m_uuid <util.random_uuid>
    - spawn dcutscene_camera_entity <[player].location> save:<[m_uuid]>
    - define camera_mount <entry[<[m_uuid]>].spawned_entity>
    - adjust <[camera_mount]> tracking_range:256
    - if <[bound]>:
      - mount <[player]>|<[camera_mount]>
    - flag <[player]> dcutscene_mount:<[camera_mount]>
    - flag <[player]> dcutscene_bound:<[bound]>
    - determine <[camera_ent]>
    - wait 1t

# Returns the first camera location in the cutscene and returns null if there is an error.
dcutscene_first_loc:
    type: procedure
    debug: false
    definitions: scene
    script:
    - define data <server.flag[dcutscenes.<[scene]>]||null>
    - if <[data]> == null:
      - determine null
    - define camera <[data.keyframes.camera]||null>
    - if <[camera]> == null:
      - determine null
    - define first <[camera].keys.first>
    - define camera_data <[camera.<[first]>]>
    - if <[camera_data.recording.bool].is_truthy>:
      - define first <[camera_data.recording.frames].keys.first>
      - define loc <[camera_data.recording.frames.<[first]>]>
      - define first_loc <location[<[loc.l]>].with_pitch[<[loc.p]>].with_yaw[<[loc.y]>]||null>
      - determine <[first_loc]>
    - else:
      - define first_loc <location[<[camera.<[first]>.location]>]||null>
      - determine <[first_loc]>

# Returns the first camera location closest to the timespot
dcutscene_timespot_loc:
    type: procedure
    debug: false
    definitions: scene|timespot
    script:
    - define data <server.flag[dcutscenes.<[scene]>]||null>
    - if !<[timespot].exists> || !<[timespot].is_decimal> || <[data]> == null:
      - determine null
    - define camera <[data.keyframes.camera]||null>
    - if <[camera]> == null:
      - determine null
    - define tick <[camera].keys.filter[is_more_than_or_equal_to[<[timespot]>]].first||0>
    - determine <[camera.<[tick]>.location]||null>

# Returns the length of the cutscene
dcutscene_length:
    type: procedure
    debug: false
    definitions: scene
    script:
    - determine <server.flag[dcutscenes.<[scene]>.length]||null>

# Returns the origin point of the cutscene
dcutscene_get_origin_point:
    type: procedure
    debug: false
    definitions: scene
    script:
    - determine <server.flag[dcutscenes.<[scene]>.settings.origin]||null>

# Makes playes hidden for the cutscene user until the cutscene is over
dcutscene_hide_players_task:
    type: task
    debug: false
    definitions: player|world
    script:
    - flag <[player]> dcutscene_hide_players:<map>
    - foreach <server.online_players.exclude[<[player]>].filter_tag[<[filter_value].world.equals[<[world]>]>]> as:p:
      - adjust <[player]> hide_entity:<[p]>
      - flag <[player]> dcutscene_hide_players.<[p].uuid>
    - while <[player].has_flag[dcutscene_hide_players]>:
      - define data <[player].flag[dcutscene_hide_players]>
      - foreach <server.online_players.exclude[<[player]>].filter_tag[<[filter_value].world.equals[<[world]>]>]> as:p:
        - if <[data.<[p].uuid>]||null> == null:
          - adjust <[player]> hide_entity:<[p]>
          - flag <[player]> dcutscene_hide_players.<[p].uuid>
      - wait 0.5s

# Cutscene Inventory give
dcutscene_scene_inventory_give:
    type: task
    debug: false
    definitions: player
    script:
    - flag <[player]> dcutscene_previous_inv:<[player].inventory.map_slots>

# Cutscene Inventory return
dcutscene_scene_inventory_return:
    type: task
    debug: false
    definitions: player
    script:
    - inventory swap d:<[player].inventory>

#=========== Animator Tasks =============
dcutscene_run_task_animator_task:
    type: task
    debug: false
    definitions: task
    script:
    - define script <script[<[task.script]>]||null>
    - if <[script]> == null:
      - debug error "Could not run task in dcutscene_run_task_animator_task script does not exist?"
    - else:
      - define waitable <[task.waitable]>
      - define defs <[task.defs]>
      - if <[defs].equals[false]>:
        - define defs <empty>
      - else:
        - define defs <[defs].parsed||<empty>>
      - define delay <duration[<[task.delay]>].in_seconds>s
      - choose <[waitable]>:
        - case true:
          - ~run <[script]> def:<[defs]> delay:<[delay]>
        - case false:
          - run <[script]> def:<[defs]> delay:<[delay]>

#=Sound Animator Task
dcutscene_animator_sound_play:
    type: task
    debug: false
    definitions: s_data|world|origin
    script:
    - define loc <[s_data.location]||false>
    - if <[loc].is_truthy> && <[origin].is_truthy>:
      - define origin_offset <[s_data.origin_offset]||0,0,0>
      - define loc <[origin].add[<[origin_offset]>]||false>
    - if !<[loc].is_truthy>:
      - define sound_to <player>
    - else:
      - define sound_to <location[<[loc]>].with_world[<[world]>]>
    - if <[s_data.custom]||false>:
      - playsound <[sound_to]> sound:<[s_data.sound]> volume:<[s_data.volume]||1.0> pitch:<[s_data.pitch]||1.0> custom
    - else:
      - playsound <[sound_to]> sound:<[s_data.sound]> volume:<[s_data.volume]||1.0> pitch:<[s_data.pitch]||1.0>

#=Screeneffect Animator Task
dcutscene_screeneffect:
    type: task
    debug: false
    definitions: player|fade_in|stay|fade_out|color
    script:
    - if !<[color].exists>:
      - define color <black>
    - else:
      - define color <&color[<[color]>]||<[color].parse_color>>
    - define title <script[dcutscenes_config].data_key[config].get[cutscene_transition_unicode]||null>
    - if <[title]> == null:
      - debug error "Could not find screeneffect unicode in dcutscene_screeneffect"
      - stop
    - title title:<[color]><[title]> fade_in:<duration[<[fade_in]>]> stay:<duration[<[stay]>]> fade_out:<duration[<[fade_out]>]> targets:<[player]>

#=Fake Block Animator Task
dcutscene_fake_block_animator:
    type: task
    debug: false
    definitions: loc|data|world|origin
    script:
    - if <[origin].is_truthy>:
      - define origin_offset <[data.origin_offset]||0,0,0>
      - define loc <[origin].add[<[origin_offset]>]>
    - else:
      - define loc <location[<[loc]>].with_world[<[world]||<player.world>>]||null>
    - if <[loc]> == null:
      - debug error "Invalid location or could not find location in dcutscene_fake_object_animator"
      - stop
    - define material <material[<[data.block]>]||null>
    - if <[material]> == null:
      - debug error "Invalid material for dcutscene_fake_object_animator"
      - stop
    - define proc <[data.procedure.script]||none>
    - define defs <[data.procedure.defs]||none>
    - if <[proc]> != none && <[defs]> != none:
      - define loc <[loc].proc[<[proc]>].context[<[defs].parsed>]||null>
    - else if <[proc]> != none:
      - define loc <[loc].proc[<[proc]>]||null>
    - if <[loc]> == null:
      - debug error "Invalid procedure or could not find procedure in dcutscene_fake_object_animator"
      - stop
    - showfake <[material]> <[loc]> players:<player> duration:<[data.duration]>

#=Fake Schematic Animator Task
dcutscene_fake_schem_animator:
    type: task
    debug: false
    definitions: loc|data|world|origin
    script:
    - if <[origin].is_truthy>:
      - define origin_offset <[data.origin_offset]||0,0,0>
      - define loc <[origin].add[<[origin_offset]>]>
    - else:
      - define loc <location[<[loc]>].with_world[<[world]||<player.world>>]||null>
    - if <[loc]> == null:
      - debug error "Invalid location for dcutscene_fake_schem_animator"
      - stop
    - define schem <[data.schem]>
    # If the schematic does not exist try to load it and if it still does not exist stop
    - if !<schematic[<[schem]>].exists>:
      - ~schematic load name:<[schem]>
      - if !<schematic[<[schem]>].exists>:
        - debug error "Invalid schematic <[schem]> for dcutscene_fake_schem_animator"
        - stop
    - define noair <[data.noair]||true>
    - define waitable <[data.waitable]||false>
    - define duration <[data.duration]||10s>
    - choose <[data.angle]||forward>:
      - case forward:
        - define angle 0
      - case backward:
        - define angle 180
      - case right:
        - define angle 90
      - case left:
        - define angle 270
    - if <[noair]> && <[waitable]>:
      - ~schematic paste name:<[schem]> fake_to:<player> fake_duration:<[duration]> <[loc]> noair angle:<[angle]>
    - else if <[noair]> && !<[waitable]>:
      - schematic paste name:<[schem]> fake_to:<player> fake_duration:<[duration]> <[loc]> noair angle:<[angle]>
    - else if !<[noair]> && <[waitable]>:
      - ~schematic paste name:<[schem]> fake_to:<player> fake_duration:<[duration]> <[loc]> angle:<[angle]>
    - else if !<[noair]> && !<[waitable]>:
      - schematic paste name:<[schem]> fake_to:<player> fake_duration:<[duration]> <[loc]> angle:<[angle]>

#=Particle Animator Task
dcutscene_particle_animator_play:
    type: task
    debug: false
    definitions: player|p_data|world|scene_uuid|origin
    script:
    #Const
    - if <[origin].is_truthy>:
      - define origin_offset <[p_data.origin_offset]||0,0,0>
      - define loc <[origin].add[<[origin_offset]>]||null>
    - else:
      - define loc <location[<[p_data.loc]>].with_world[<[world]>]||null>
    - if <[loc]> == null:
      - debug error "Invalid location in dcutscene_particle_animator_play"
      - stop
    - define particle_loc <[loc]>
    - define repeat_count <[p_data.repeat]>
    - define repeat_interval <[p_data.repeat_interval]>
    - define particle <[p_data.particle]>
    - define special_data <[p_data.special_data].parsed>
    - define range <[p_data.range]>
    - define quantity <[p_data.quantity]>
    - define offset <[p_data.offset]>
    - define proc_script <[p_data.procedure.script]>
    - define proc_defs <[p_data.procedure.defs].parsed>
    - define velocity <location[<[p_data.velocity]>]>
    #Animator
    - repeat <[repeat_count]>:
      - if !<[player].is_online> || <[player].flag[dcutscene_played_scene.uuid]||null> != <[scene_uuid]>:
        - stop
      - if <[proc_script].is_truthy>:
        - if <[proc_defs].is_truthy>:
          - define loc <[particle_loc].proc[<[proc_script]>].context[<[proc_defs]>]||null>
        - else:
          - define loc <[particle_loc].proc[<[proc_script]>]||null>
        - if <[loc]> == null:
          - debug error "Invalid procedure or could not find procedure to determine a location in dcutscene_particle_animator_play"
          - stop
      - if <[special_data].is_truthy>:
        - playeffect effect:<[particle]> at:<[loc]> special_data:<[special_data]> visibility:<[range]> quantity:<[quantity]> offset:<[offset]> velocity:<[velocity]> targets:<player>
      - else:
        - playeffect effect:<[particle]> at:<[loc]> visibility:<[range]> quantity:<[quantity]> offset:<[offset]> velocity:<[velocity]> targets:<player>
      - wait <[repeat_interval]>

#=Command Animator Task
dcutscene_command_animator:
    type: task
    debug: false
    definitions: c_data
    script:
    - define command <[c_data.command].parsed>
    - choose <[c_data.execute_as]>:
      - case player:
        - choose <[c_data.silent]>:
          - case true:
            - execute as_player <[command]> silent
          - case false:
            - execute as_player <[command]>
      - case server:
        - choose <[c_data.silent]>:
          - case true:
            - execute as_server <[command]> silent
          - case false:
            - execute as_server <[command]>

#=Time Animator Task
dcutscene_time_animator:
    type: task
    debug: false
    definitions: t_data
    script:
    - if <[t_data.reset]>:
      - time player reset
    - else:
      - choose <[t_data.freeze]>:
        - case true:
          - time player <[t_data.time]> reset:<[t_data.duration]> freeze
        - case false:
          - time player <[t_data.time]> reset:<[t_data.duration]>

#======== Cutscene Animation Stop ============
dcutscene_animation_stop:
    type: task
    debug: false
    definitions: player
    script:
    - define player <[player]||<player>>
    - inventory swap d:<[player].inventory> o:<[player].flag[dcutscene_played_scene_inv].if_null[<player.inventory>]>
    - cast INVISIBILITY remove
    - adjust <[player]> stop_sound
    - adjust <[player]> gamemode:<[player].flag[dcutscene_previous_gamemode]>
    - adjust <[player]> spectate:<[player]>
    - if <[player].has_flag[dcutscene_bars]>:
      - ~run dcutscene_bars_remove def.player:<[player]>
    #Hidden players are shown
    - if <[player].has_flag[dcutscene_hide_players]>:
      - foreach <[player].flag[dcutscene_hide_players]> key:uuid:
        - adjust <[player]> show_entity:<player[<[uuid]>]>
      - flag <[player]> dcutscene_hide_players:!
    #Camera removal
    - if <[player].has_flag[dcutscene_camera]>:
      - define bound <[player].flag[dcutscene_bound]>
      - if <[bound]>:
        - mount cancel <[player]>
        - teleport <[player]> <[player].flag[dcutscene_camera].location.above[0.6]>
      - remove <[player].flag[dcutscene_mount]>
      - remove <[player].flag[dcutscene_camera]>
      - flag <[player]> dcutscene_mount:!
      - flag <[player]> dcutscene_camera:!
      - flag <[player]> dcutscene_bound:!
    #Spawned models removal
    - foreach <[player].flag[dcutscene_spawned_models]||<list>> key:id as:model:
      - choose <[model.type]>:
        - case player_model:
          - run pmodels_remove_model def:<[model.root]>
        - case model:
          - run dmodels_delete def:<[model.root]>
    - flag <[player]> dcutscene_played_scene:!
    - flag <[player]> dcutscene_spawned_models:!
    - flag <[player]> dcutscene_timespot:!

#========= Path movement for camera, models, and entities ========
#This moves the camera or models across the designated path
#Note that the mount must be out of range of interaction or "cannot interact with self error will occur"
dcutscene_path_move:
    type: task
    debug: false
    definitions: cutscene|timespot|scene_uuid|entity|type|data|world|origin
    script:
    - define cutscene <server.flag[dcutscenes.<[cutscene]>]||null>
    - if <[cutscene]> != null:
      - define world <[world]||<player.world>>
      - define type <[type]||null>
      - define scene_uuid <[scene_uuid]||null>
      - if <[type]> != null:
        - choose <[type]>:
          #======================= Camera Path Move ===========================
          - case camera:
            #=Preparation
            - define mount <player.flag[dcutscene_mount]>
            - define bound <player.flag[dcutscene_bound]>
            - define keyframes <[cutscene.keyframes.camera]>
            #Before
            - foreach <[keyframes]> key:c_id as:keyframe:
              - if <player.vehicle||null> != <[mount]> && <[bound]>:
                - mount <player>|<[mount]>
              #Time
              - define time_1 <[keyframe.tick]||null>
              - if <[time_1]> == null:
                - debug error "Could not determine time_1 for frame <[c_id]> on scene <[cutscene.name]>"
                - foreach next
              #Skip frames according to timespot
              - if <[time_1].is_less_than[<[timespot]>]>:
                - foreach next
              #Inverted camera
              - define invert <[keyframe.invert]||false>
              #Look location and bool
              - define eye_loc <[keyframe.eye_loc.location]>
              - define eye_loc_bool <[keyframe.eye_loc.boolean]||false>
              #=Recording animator
              - define rec <[keyframe.recording]||false>
              - define rec_bool <[rec.bool]||false>
              - if <[rec_bool]>:
                #Length of recording
                - repeat <[rec.length]> as:t:
                  - if !<player.is_online>:
                    - stop
                  #Check if player is in another cutscene if so stop
                  - if <player.flag[dcutscene_played_scene.uuid]||null> != <[scene_uuid]>:
                    - stop
                  - define r <[rec.frames.<[t]>]>
                  #If a look location has been set
                  - if <[eye_loc_bool]>:
                    - define look_loc <[entity].eye_location.face[<[eye_loc].with_world[<[world]>]>]>
                    - define yaw <[look_loc].yaw>
                    - define pitch <[look_loc].pitch>
                  - else:
                    - define yaw <[r.y]>
                    - define pitch <[r.p]>
                  - if <[origin].is_truthy>:
                    - define l <[origin].add[<[r.o]||0,0,0>]>
                  - else:
                    - define l <location[<[r.l]>].with_yaw[<[yaw]>].with_world[<[world]>]>
                  - if <[invert]>:
                    - teleport <[entity]> <[l].below[0.15].rotate_yaw[180].with_pitch[<[pitch].add[180]>]>
                  - else:
                    - teleport <[entity]> <[l].below[0.15].with_pitch[<[pitch]>]>
                  #This is an easy solution to the cannot interact with player error
                  - if <[bound]>:
                    - look <player> <[entity].location.relative[0,0,-5].rotate_yaw[180].with_pitch[0]> duration:1t
                  - teleport <[mount]> <[entity].location.relative[0,1,-1]>
                  - wait 1t
              #=Default animator calculations
              - else:
                - define interpolation <[keyframe.interpolation]>
                - define offset <[keyframe.origin_offset]||0,0,0>
                - if <[origin].is_truthy>:
                  - define loc_1 <[origin].add[<[offset]>]||null>
                - else:
                  - define loc_1 <location[<[keyframe.location]>]||null>
                - if <[loc_1]> == null:
                  - foreach next
                #Constants
                - define interp_look <[keyframe.interpolate_look]||true>
                - define move <[keyframe.move]>
                - define rotate_mul <[keyframe.rotate_mul]||1.0>
                #After
                - foreach <[keyframes]> key:2_id as:2_keyframe:
                  - if <[2_keyframe.tick].is_more_than[<[time_1]>]>:
                    - define time_2 <[2_keyframe.tick]>
                    - define loc_2 <location[<[2_keyframe.location]>]||null>
                    - define offset_after <[2_keyframe.origin_offset]||0,0,0>
                    - define eye_loc_2 <location[<[2_keyframe.eye_loc.location]>]||null>
                    - foreach stop
                - if <[origin].is_truthy>:
                  - define loc_2 <[origin].add[<[offset_after]>]||null>
                - else:
                  - define loc_2 <[loc_2]||null>
                - define time_2 <[time_2]||null>
                - if <[loc_2]> == null || <[time_2]> == null:
                  - foreach next
                - define time <[time_2].sub[<[time_1]>]>
                - if <[interpolation]> == smooth:
                  #After Extra
                  - foreach <[keyframes]> key:a_e_id as:a_e_keyframe:
                    - if <[a_e_keyframe.tick].is_more_than[<[time_2]>]>:
                      - define loc_2_after <[a_e_keyframe.location]||<[loc_2]>>
                      - define offset_after_extra <[a_e_keyframe.offset]||<[offset_after]>>
                      - foreach stop
                  - if <[origin].is_truthy>:
                    - define loc_2_after <[origin].add[<[offset_after_extra]||0,0,0>]>
                  - else:
                    - define loc_2_after <[loc_2_after].as[location]>
                  #Before Extra
                  - foreach <[keyframes]> key:b_e_id as:b_e_keyframe:
                    - if <[b_e_keyframe.tick].is_less_than[<[time_1]>]>:
                      - define list:->:<[b_e_keyframe]>
                  - define prev_data <[list].last>
                  - if <[origin].is_truthy>:
                    - define loc_1_prev <[origin].add[<[prev_data.origin_offset]||<[offset]>>]>
                  - else:
                    - define loc_1_prev <[prev_data.location].as[location]||<[loc_1]>>

                #=Animation
                - repeat <[time]>:
                  - if !<player.is_online>:
                    - stop
                  - if !<player.has_flag[dcutscene_played_scene.uuid]>:
                    - stop
                  - if <player.flag[dcutscene_played_scene.uuid]||null> != <[scene_uuid]>:
                    - stop
                  - if <[entity].is_spawned>:
                    - define time_index <[value]>
                    - define time_percent <[time_index].div[<[time]>]>
                    #Move true
                    - if <[move]>:
                      - if <[time_index]> < <[time]>:
                        - choose <[interpolation]>:
                          #Linear Interpolation
                          - case linear:
                            - define tp_loc <[loc_2].sub[<[loc_1]>].mul[<[time_percent]>].add[<[loc_1]>]>
                          #Catmullrom Interpolation
                          - case smooth:
                            - define p0 <[loc_1_prev]>
                            - define p1 <[loc_1]>
                            - define p2 <[loc_2]>
                            - define p3 <[loc_2_after]>
                            #Catmullrom calc
                            - define tp_loc <proc[dcutscene_catmullrom_proc].context[<[p0]>|<[p1]>|<[p2]>|<[p3]>|<[time_percent]>]>
                        #Interp Look True
                        - if <[interp_look]>:
                          - if <[time_index]> < <[time]>:
                            #Eye location interpolation multiplier
                            - define interp_mul <[time_percent].mul[<[rotate_mul]>]>
                            - if <[interp_mul]> > 1:
                              - define interp_mul 1.0
                            - define yaw <[eye_loc_2].yaw.sub[<[eye_loc].yaw>].mul[<[interp_mul]>].add[<[eye_loc].yaw>]>
                            - define pitch <[eye_loc_2].pitch.sub[<[eye_loc].pitch>].mul[<[interp_mul]>].add[<[eye_loc].pitch>]>
                          - else:
                            - define yaw <[eye_loc_2].yaw>
                            - define pitch <[eye_loc_2].pitch>
                        #Interp Look False
                        - else:
                          #Location 1 yaw and pitch
                          - define yaw <[eye_loc].yaw>
                          - define pitch <[eye_loc].pitch>
                      - else:
                        - define tp_loc <[loc_2]>
                        - define yaw <[eye_loc_2].yaw>
                        - define pitch <[eye_loc_2].pitch>
                      - define mount_loc <[tp_loc]>
                    #Move false
                    - else:
                      - define tp_loc <[loc_1]>
                      - if <[time_index]> < <[time]>:
                        - if <[interp_look]>:
                          #Eye location interpolation multiplier
                          - define interp_mul <[time_percent].mul[<[rotate_mul]>]>
                          - if <[interp_mul]> > 1:
                            - define interp_mul 1.0
                          - define yaw <[eye_loc_2].yaw.sub[<[eye_loc].yaw>].mul[<[interp_mul]>].add[<[eye_loc].yaw>]>
                          - define pitch <[eye_loc_2].pitch.sub[<[eye_loc].pitch>].mul[<[interp_mul]>].add[<[eye_loc].pitch>]>
                        - else:
                          - define yaw <[eye_loc].yaw>
                          - define pitch <[eye_loc].pitch>
                          - define mount_loc <[loc_2]>
                      - else:
                        - define yaw <[eye_loc_2].yaw>
                        - define pitch <[eye_loc_2].pitch>
                        - define tp_loc <[loc_2]>
                      - define mount_loc <[loc_2].as[location].sub[<[loc_1]>].mul[<[time_percent]>].add[<[loc_1]>]>
                    #If a look location has been set
                    - if <[eye_loc_bool]>:
                      - define look_loc <[entity].eye_location.face[<[eye_loc].with_world[<[world]>]>]>
                      - define yaw <[look_loc].yaw>
                      - define pitch <[look_loc].pitch>
                    #Camera
                    - if <[invert]>:
                      - teleport <[entity]> <[tp_loc].below[0.15].with_yaw[<[yaw]>].rotate_yaw[180].with_pitch[<[pitch].add[180]>].with_world[<[world]>]>
                    - else:
                      - teleport <[entity]> <[tp_loc].below[0.15].with_yaw[<[yaw]>].with_pitch[<[pitch]>].with_world[<[world]>]>
                    #Mount
                    - define mount_loc <[entity].location.relative[0,0,-1]>
                    - teleport <[mount]> <[mount_loc]>
                    - if <[bound]>:
                      - look <player> <[entity].location.relative[0,0,-5].rotate_yaw[180].with_pitch[0]> duration:1t
                  - else:
                    - stop
                  - wait 1t

          #====================== Model Path Move =======================
          - case model:
            #=Preparation
            - define keyframes <[cutscene.keyframes.models.<[data.tick]>.<[data.uuid]>.path]>
            - foreach <[keyframes]> key:tick_id as:keyframe:
              - define time_1 <[keyframe.tick]||null>
              - if !<[time_1].is_integer>:
                - foreach next
              #Skip frames according to timespot
              - if <[time_1].is_less_than[<[timespot]>]>:
                - foreach next
              - define interpolation <[keyframe.interpolation]>
              - define move <[keyframe.move]>
              - define loc_1 <[keyframe.location]>
              - define rotate_interp <[keyframe.rotate_interp]>
              - define rotate_mul <[keyframe.rotate_mul]>
              - define ray_trace <[keyframe.ray_trace]>
              - define animation <[keyframe.animation]>
              #Model Animation
              - if <[animation]> != false && <[animation]> != stop:
                - run dmodels_animate def:<[entity]>|<[animation]>
                - define loop <server.flag[dmodels_data.animations_<[entity].flag[dmodel_model_id]>.<[animation]>.loop]||false>
                - if <[loop]> == hold:
                  - flag <[entity]> dcutscene_model_animation_state:hold
                - else:
                  - flag <[entity]> dcutscene_model_animation_state:!
              - else if <[animation]> == stop:
                - run dmodels_end_animation def:<[entity]>
              #Reset position
              - if !<[entity].has_flag[dcutscene_model_animation_state]> || <[entity].flag[dcutscene_model_animation_state]> != hold:
                - run dmodels_reset_model_position def:<[entity]>
              #After
              - foreach <[keyframes]> key:aft_tick_id as:aft_keyframe:
                - if <[aft_tick_id].is_more_than[<[time_1]>]>:
                  - define time_2 <[aft_tick_id]>
                  - define loc_2 <[aft_keyframe.location]||<[loc_1]>>
                  - foreach stop
              - define loc_2 <[loc_2]||<[loc_1]>>
              - define time_2 <[time_2]||null>
              #Time calculation
              - define time <[time_2].sub[<[time_1]>]||null>
              - if <[time]> == null:
                - teleport <[entity]> <[loc_2].with_yaw[<[loc_2].yaw>]>
                - foreach next
              - if <[interpolation]> == smooth:
                #Before Extra
                - foreach <[keyframes]> key:b_e_id as:b_e_keyframe:
                  - if <[b_e_id].is_less_than[<[time_1]>]>:
                    - define list:->:<[b_e_keyframe]>
                - define loc_1_prev <[list].last.get[location]||<[loc_1]>>
                #After extra
                - foreach <[keyframes]> key:aft_e_tick_id as:aft_e_keyframe:
                  - if <[aft_e_tick_id].is_more_than[<[time_2]>]>:
                    - define loc_2_after <[aft_e_keyframe.location]||<[loc_2]>>
                    - foreach stop
                - define loc_2_after <[loc_2_after]||<[loc_2]>>

              #=Animation
              - repeat <[time]>:
                - if <player.flag[dcutscene_played_scene.uuid]||null> != <[scene_uuid]>:
                  - stop
                - chunkload <[entity].location.with_world[<[world]>].chunk> duration:1t
                - if <[entity].is_spawned>:
                  - define time_index <[value]>
                  - define time_percent <[time_index].div[<[time]>]>
                  - if <[move]>:
                    - if <[time_index]> < <[time]>:
                      #Rotation Interpolation
                      - if <[rotate_interp]>:
                        - define interp_mul <[time_percent].mul[<[rotate_mul]>]>
                        - if <[interp_mul]> > 1:
                          - define interp_mul 1.0
                        - define yaw <[loc_2].yaw.sub[<[loc_1].yaw>].mul[<[interp_mul]>].add[<[loc_1].yaw>]>
                      - else:
                        - define yaw <[loc_2].yaw>
                      #Path Interpolation
                      - choose <[interpolation]>:
                        - case linear:
                          #Linear Interpolation calc
                          - define data <[loc_2].as[location].sub[<[loc_1]>].mul[<[time_percent]>].add[<[loc_1]>]>
                        - case smooth:
                          - define p0 <[loc_1_prev].as[location]>
                          - define p1 <[loc_1].as[location]>
                          - define p2 <[loc_2].as[location]>
                          - define p3 <[loc_2_after].as[location]>
                          #Catmullrom calc
                          - define data <proc[dcutscene_catmullrom_proc].context[<[p0]>|<[p1]>|<[p2]>|<[p3]>|<[time_percent]>]>
                      #Ray Trace
                      - if <[ray_trace.direction].is_truthy>:
                        - choose <[ray_trace.direction]||floor>:
                          - case floor:
                            - define ray <[data].above[0.5].with_pitch[90].ray_trace[range=60;fluids=<[ray_trace.liquid]||false>;nonsolids=<[ray_trace.passable]||false>]||null>
                            - if <[ray]> != null:
                              - define data <[ray]>
                          - case ceiling:
                            - define ray <[data].above[0.5].with_pitch[-90].ray_trace[range=60;fluids=<[ray_trace.liquid]||false>;nonsolids=<[ray_trace.passable]||false>]||null>
                            - if <[ray]> != null:
                              - define data <[ray]>
                      - teleport <[entity]> <[data].with_pitch[0].with_yaw[<[yaw]>].with_world[<[world]>]>
                    - else:
                      - teleport <[entity]> <[loc_2].with_world[<[world]>]>
                  - else:
                    - teleport <[entity]> <[loc_1].with_world[<[world]>]>
                  - wait 1t
              - adjust <[loc_2].with_world[<[world]>].chunk> load
              - teleport <[entity]> <[loc_2].with_yaw[<[loc_2].yaw>].with_world[<[world]>]>

          #====================== Player Model Path Move =======================
          - case player_model:
            #= Preparation
            - define keyframes <[cutscene.keyframes.models.<[data.tick]>.<[data.uuid]>.path]>
            - foreach <[keyframes]> key:tick_id as:keyframe:
              - define time_1 <[keyframe.tick]||null>
              - if !<[time_1].is_integer>:
                - foreach next
              #Skip frames according to timespot
              - if <[time_1].is_less_than[<[timespot]>]>:
                - foreach next
              - define interpolation <[keyframe.interpolation]>
              - define move <[keyframe.move]>
              - define loc_1 <[keyframe.location]>
              - define rotate_interp <[keyframe.rotate_interp]>
              - define rotate_mul <[keyframe.rotate_mul]>
              - define ray_trace <[keyframe.ray_trace]>
              - define animation <[keyframe.animation]>
              - define skin <[keyframe.skin]||none>
              #Model Animation
              - if <[animation]> != false && <[animation]> != stop:
                - if <[entity].has_flag[external_parts]>:
                  - run pmodels_remove_external_parts def:<[entity]>
                - run pmodels_animate def:<[entity]>|<[animation]>
                - define loop <server.flag[pmodels_data.animations_<[entity].flag[pmodel_model_id]>.<[animation]>.loop]||false>
                - if <[loop]> == hold:
                  - flag <[entity]> dcutscene_model_animation_state:hold
                - else:
                  - flag <[entity]> dcutscene_model_animation_state:!
              - else if <[animation]> == stop:
                - run pmodels_end_animation def:<[entity]>
              #Reset position
              - if !<[entity].has_flag[dcutscene_model_animation_state]> || <[entity].flag[dcutscene_model_animation_state]> != hold:
                - run pmodels_reset_model_position def:<[entity]>
              #Skin
              - if <[skin]> != none && <[loop_index]> != 1:
                - if <[skin]> == player:
                  - define skin <player>
                - else:
                  - define skin <[skin].parsed||<[skin]>>
                - run pmodels_change_skin def:<[skin]>|<[entity]>
              #After
              - foreach <[keyframes]> key:aft_tick_id as:aft_keyframe:
                - if <[aft_tick_id].is_more_than[<[time_1]>]>:
                  - define time_2 <[aft_tick_id]>
                  - define loc_2 <[aft_keyframe.location]||<[loc_1]>>
                  - foreach stop
              - define loc_2 <[loc_2]||<[loc_1]>>
              - define time_2 <[time_2]||null>
              #Time
              - define time <[time_2].sub[<[time_1]>]||null>
              - if <[time]> == null:
                - teleport <[entity]> <[loc_2].with_yaw[<[loc_2].yaw>]>
                - foreach next
              - if <[interpolation]> == smooth:
                #Before Extra
                - foreach <[keyframes]> key:b_e_id as:b_e_keyframe:
                  - if <[b_e_id].is_less_than[<[time_1]>]>:
                    - define list:->:<[b_e_keyframe]>
                - define loc_1_prev <[list].last.get[location]||<[loc_1]>>
                #After extra
                - foreach <[keyframes]> key:aft_e_tick_id as:aft_e_keyframe:
                  - if <[aft_e_tick_id].is_more_than[<[time_2]>]>:
                    - define loc_2_after <[aft_e_keyframe.location]||<[loc_2]>>
                    - foreach stop
                - define loc_2_after <[loc_2_after]||<[loc_2]>>

              #=Animation
              - repeat <[time]>:
                - if <player.flag[dcutscene_played_scene.uuid]||null> != <[scene_uuid]>:
                  - stop
                - chunkload <[entity].location.with_world[<[world]>].chunk> duration:1t
                - if <[entity].is_spawned>:
                  - define time_index <[value]>
                  - define time_percent <[time_index].div[<[time]>]>
                  - if <[move]||false>:
                    - if <[time_index]> < <[time]>:
                      #Rotation Interpolation
                      - if <[rotate_interp]>:
                        - define interp_mul <[time_percent].mul[<[rotate_mul]>]>
                        - if <[interp_mul]> > 1:
                          - define interp_mul 1.0
                        - define yaw <[loc_2].yaw.sub[<[loc_1].yaw>].mul[<[interp_mul]>].add[<[loc_1].yaw>]>
                      - else:
                        - define yaw <[loc_2].yaw>
                      #Path Interpolation
                      - choose <[interpolation]>:
                        - case linear:
                          #Linear Interpolation calc
                          - define data <[loc_2].as[location].sub[<[loc_1]>].mul[<[time_percent]>].add[<[loc_1]>]>
                        - case smooth:
                          - define p0 <[loc_1_prev].as[location]>
                          - define p1 <[loc_1].as[location]>
                          - define p2 <[loc_2].as[location]>
                          - define p3 <[loc_2_after].as[location]>
                          #Catmullrom calc
                          - define data <proc[dcutscene_catmullrom_proc].context[<[p0]>|<[p1]>|<[p2]>|<[p3]>|<[time_percent]>]>
                      #Ray Trace
                      - if <[ray_trace.direction].is_truthy>:
                        - choose <[ray_trace.direction]||floor>:
                          - case floor:
                            - define ray <[data].above[0.5].with_pitch[90].ray_trace[range=60;fluids=<[ray_trace.liquid]||false>;nonsolids=<[ray_trace.passable]||false>]||null>
                            - if <[ray]> != null:
                              - define data <[ray]>
                          - case ceiling:
                            - define ray <[data].above[0.5].with_pitch[-90].ray_trace[range=60;fluids=<[ray_trace.liquid]||false>;nonsolids=<[ray_trace.passable]||false>]||null>
                            - if <[ray]> != null:
                              - define data <[ray]>
                      - teleport <[entity]> <[data].with_pitch[0].with_yaw[<[yaw]>].with_world[<[world]>]>
                    - else:
                      - teleport <[entity]> <[loc_2].with_world[<[world]>]>
                  - else:
                    - teleport <[entity]> <[loc_1].with_world[<[world]>]>
                  - wait 1t
              - adjust <[loc_2].with_world[<[world]>].chunk> load
              - teleport <[entity]> <[loc_2].with_yaw[<[loc_2].yaw>].with_world[<[world]>]>

#======= Cutscene Path Shower Interval =======
#Interval for path shower
dcutscene_path_show_interval:
    type: task
    debug: false
    definitions: type|tick|uuid
    script:
    - define script <script[dcutscenes_config].data_key[config].get[cutscene_path_update_interval]||4s>
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - define duration <duration[<[script]>]||3s>
    - define data <player.flag[cutscene_data]>
    - define data_world <[data.world]>
    - define data_name <[data.name]>
    - choose <[type]>:
      - case camera:
        - define text "Showing camera path for scene <green><[data.name]><gray>."
        - define text_2 "<gray>To stop showing the path chat <red>stop<gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - narrate <[text_2]>
        - flag <player> cutscene_modify:camera_path expire:30m
        - while <player.flag[cutscene_modify]||> == camera_path:
          - if <player.is_online> && <[data_world].contains[<player.world.name>]>:
            - run dcutscene_path_show def:<[data_name]>|camera
            - wait <[duration]>
      - case player_model:
        - define root_data <[data.keyframes.models.<[tick]>.<[uuid]>.root]||none>
        - if <[root_data]> != none:
          - define tick <[root_data.tick]>
          - define uuid <[root_data.uuid]>
        - define text "Showing player model path for <green><[data.keyframes.models.<[tick]>.<[uuid]>.id]> <gray>in scene <green><[data.name]><gray>."
        - define text_2 "<gray>To stop showing the path chat <red>stop<gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - narrate <[text_2]>
        - flag <player> cutscene_modify:player_model_path expire:30m
        - while <player.flag[cutscene_modify]||> == player_model_path:
          - if <player.is_online> && <[data_world].contains[<player.world.name>]>:
            - run dcutscene_path_show def:<[data_name]>|player_model|<[tick]>|<[uuid]>
            - wait <[duration]>
      - case model:
        - define root_data <[data.keyframes.models.<[tick]>.<[uuid]>.root]||none>
        - if <[root_data]> != none:
          - define tick <[root_data.tick]>
          - define uuid <[root_data.uuid]>
        - define text "Showing model path for <green><[data.keyframes.models.<[tick]>.<[uuid]>.id]> <gray>in scene <green><[data.name]><gray>."
        - define text_2 "<gray>To stop showing the path chat <red>stop<gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - narrate <[text_2]>
        - flag <player> cutscene_modify:model_path expire:30m
        - while <player.flag[cutscene_modify]||> == model_path:
          - if <player.is_online> && <[data_world].contains[<player.world.name>]>:
            - run dcutscene_path_show def:<[data_name]>|model|<[tick]>|<[uuid]>
            - wait <[duration]>

#========== Cutscene Path Shower ===========
#Shows the path of the camera or model path
dcutscene_path_show:
    type: task
    debug: false
    definitions: cutscene|type|tick|uuid
    script:
    - define data <server.flag[dcutscenes.<[cutscene]>]||null>
    - if <[data]> != null:
      - define path_material <material[<script[dcutscenes_config].data_key[config].get[cutscene_path_material]>]||<material[barrier]>>
      - choose <[type]>:
        - case camera:
          - define keyframes <[data.keyframes.camera]>
        - case player_model:
          - define keyframes <[data.keyframes.models.<[tick]>.<[uuid]>.path]>
        - case model:
          - define keyframes <[data.keyframes.models.<[tick]>.<[uuid]>.path]>
      - define dist <script[dcutscenes_config].data_key[config].get[cutscene_path_distance]||50>
      - foreach <[keyframes]> key:id as:keyframe:
        #If there are record frames display those
        - define record <[keyframe.recording.bool]||false>
        - if <[record]>:
          - define path <[data.name].proc[dcutscene_recording_frames_path_creator].context[<[keyframe.tick]>|<player>|<player.world>]>
          - playeffect effect:block_marker special_data:<[path_material]> at:<[path]> offset:0,0,0 visibility:<[dist]> targets:<player>
        #Regular interpolated frames
        - else:
          - define interpolation <[keyframe.interpolation]>
          - define time_1 <[keyframe.tick]>
          - define loc_1 <[keyframe.location]>
          #after & time 2
          - foreach <[keyframes]> key:a_id as:a_keyframe:
            - if <[a_keyframe.tick].is_more_than[<[time_1]>]>:
              - define time_2 <[a_keyframe.tick]>
              - define loc_2 <[a_keyframe.location]>
              - foreach stop
          - define loc_2 <[loc_2]||null>
          - if <[loc_2]> == null:
            - foreach next
          - choose <[interpolation]>:
            #Linear Interpolation
            - case linear:
              #time
              - define time <[time_2].sub[<[time_1]>]>
              - define path <player.proc[dcutscene_path_creator].context[<[loc_1]>|<[loc_2]>|linear|<[time]>]||null>
              - if <[path]> != null:
                - define ploc <player.location>
                - foreach <[path]> as:point:
                  #For optimization it should only play when the player is facing the location and is within range
                  - define p_2 <[path].get[<[loop_index].add[1]>]||<[point]>>
                  - define p_loc <location[<[point]>].points_between[<[p_2]>].distance[1.5]>
                  - if !<[p_loc].is_empty>:
                    - foreach <[p_loc]> as:p_b:
                      - playeffect effect:block_marker special_data:<[path_material]> at:<[p_b]> offset:0,0,0 visibility:<[dist]> targets:<player>
            #Catmullrom Interpolation
            - case smooth:
              #after extra
              - foreach <[keyframes]> key:a_e_id as:a_e_keyframe:
                - if <[a_e_keyframe.tick].is_more_than[<[time_2]>]>:
                  - define loc_2_after <[a_e_keyframe.location]>
                  - foreach stop
              - define loc_2_after <[loc_2_after]||<[loc_2]>>
              #before extra
              - foreach <[keyframes]> key:b_e_id as:b_e_keyframe:
                - if <[b_e_keyframe.tick].is_less_than[<[time_1]>]>:
                  - define list:->:<[b_e_keyframe]>
              - define loc_1_prev <[list].last.get[location]||<[loc_1]>>
              #time
              - define time <[time_2].sub[<[time_1]>]>
              - define path <player.proc[dcutscene_path_creator].context[<[loc_1]>|<[loc_2]>|smooth|<[time]>|<[loc_1_prev]>|<[loc_2_after]>]||null>
              - if <[path]> != null:
                - playeffect effect:block_marker special_data:<[path_material]> at:<[path]> offset:0,0,0 visibility:<[dist]> targets:<player>
            - default:
              - debug error "Could not determine interpolation type in dcutscene_path_show"
    - else:
      - debug error "Could not find cutscene in dcutscene_path_show"

#======= Cutscene Semi Path Shower =========

# This is used when changing model animator path locations
dcutscene_semi_path_show:
    type: task
    debug: false
    definitions: loc|tick|tick_2|uuid
    script:
    #Center (After)
    - define loc_2 <[loc]||null>
    #After time
    - define time_2 <[tick_2]||null>
    - if <[loc_2]> != null:
      - define dist <script[dcutscenes_config].data_key[config].get[cutscene_path_distance]||50>
      - define data <player.flag[cutscene_data]>
      - define uuid <[uuid]||null>
      - if <[time_2]> == null || <[uuid]> == null:
        - stop
      - define model_data <[data.keyframes.models]||null>
      - if <[model_data]> == null:
        - stop
      #Gather the path data
      - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
      - if <[root_data]> != none:
        - define path <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path]||null>
      - else:
        - define path <[model_data.<[tick]>.<[uuid]>.path]||null>
      - if <[path]> == null:
        - stop
      #Before
      - foreach <[path]> key:tick_id as:frame:
        - if <[tick_id].is_less_than[<[time_2]>]>:
          - define frame.tick <[tick_id]>
          - define frame_list:->:<[frame]>
      - define loc_1 <[frame_list].last.get[location]||null>
      - define time_1 <[frame_list].last.get[tick]||<[time_2]>>
      - define interpolation <[frame_list].last.get[interpolation]||linear>
      - if <[loc_1]> == null:
        - stop
      - define time <[time_2].sub[<[time_1]>]>
      - choose <[interpolation]>:
        #Linear Interpolation
        - case linear:
          - define path <player.proc[dcutscene_path_creator].context[<[loc_1]>|<[loc_2]>|linear|<[time]>]||null>
          - if <[path]> != null:
            - playeffect effect:glow at:<[path]> offset:0,0,0 visibility:<[dist]> targets:<player>
        #Catmullrom Interpolation
        - case smooth:
          #After Extra
          - foreach <[path]> key:tick_id as:frame:
            - if <[tick_id].is_more_than[<[time_2]>]>:
              - define loc_2_after <[frame.location]>
              - foreach stop
          - define loc_2_after <[loc_2_after]||<[loc_2]>>
          #Before Extra
          - foreach <[path]> key:tick_id as:frame:
            - if <[tick_id].is_less_than[<[time_1]>]>:
              - define frame_list:->:<[frame]>
          - define loc_before <[frame_list].last.get[location]||<[loc_1]>>
          - define path <player.proc[dcutscene_path_creator].context[<[loc_1]>|<[loc_2]>|smooth|<[time]>|<[loc_before]>|<[loc_2_after]>]||null>
          - if <[path]> != null:
            - playeffect effect:glow at:<[path]> offset:0,0,0 visibility:<[dist]> targets:<player>

# Creates a list of path points using interpolation methods
dcutscene_path_creator:
    type: procedure
    debug: false
    definitions: player|loc_1|loc_2|type|time|loc_1_prev|loc_2_after
    script:
    - define time <[time]||null>
    - define dist <script[dcutscenes_config].data_key[config].get[cutscene_path_distance]||50>
    - define p_loc <[player].location>
    - if <[time]> != null:
      - choose <[type]>:
        #Linear Interpolation
        - case linear:
          - repeat <[time]>:
            - define time_index <[value]>
            - if <[time_index]> < <[time]>:
              - define time_percent <[time_index].div[<[time]>]>
              #Lerp calc
              - define data <[loc_2].as[location].sub[<[loc_1]>].mul[<[time_percent]>].add[<[loc_1]>]>
            - else:
              - define data <[loc_2].as[location]>
            #This ensures points that are not visible will not show particles for optimization
            - if <[p_loc].facing[<[data]>].degrees[60]> && <[p_loc].distance[<[data]>]> <= <[dist].mul[2.5]>:
              #Input data to path list
              - define points:->:<[data]>
          - determine <[points]||<list>>
        #Catmullrom Interpolation
        - case smooth:
          - repeat <[time]>:
            - define time_index <[value]>
            - if <[time_index]> < <[time]>:
              - define time_percent <[time_index].div[<[time]>]>
              - define p0 <[loc_1_prev].as[location]>
              - define p1 <[loc_1].as[location]>
              - define p2 <[loc_2].as[location]>
              - define p3 <[loc_2_after].as[location]>
              #Catmullrom calc
              - define data <proc[dcutscene_catmullrom_proc].context[<[p0]>|<[p1]>|<[p2]>|<[p3]>|<[time_percent]>]>
            - else:
              - define data <[loc_2_after].as[location]>
            - if <[p_loc].facing[<[data]>].degrees[60]> && <[p_loc].distance[<[data]>]> <= <[dist].mul[2.5]>:
              #Input data to path list
              - define points:->:<[data]>
          - determine <[points]||<list>>
        - default:
          - debug error "Could not determine interpolation type in dcutscene_path_creator"
    - else:
      - determine null

# Used to show path in camera recorder frames
dcutscene_recording_frames_path_creator:
    type: procedure
    debug: false
    definitions: scene|timespot|player|world
    script:
    - define data <server.flag[dcutscenes.<[scene]>]||null>
    - if <[data]> == null:
      - determine <list>
    - define dist <script[dcutscenes_config].data_key[config].get[cutscene_path_distance]||50>
    - define rec_frames <[data.keyframes.camera.<[timespot]>.recording.frames]||<map>>
    - define p_loc <[player].location>
    - foreach <[rec_frames]> key:rt as:r:
      - define l <location[<[r.l]>].with_world[<[world]>]>
      - if <[p_loc].facing[<[l]>].degrees[60]> && <[p_loc].distance[<[l]>]> <= <[dist].mul[2.5]>:
        - define points:->:<[l]>
    - determine <[points]||<list>>

dcutscene_catmullrom_get_t:
    type: procedure
    debug: false
    definitions: t|p0|p1
    script:
    # This is more complex for different alpha values, but alpha=1 compresses down to a '.vector_length' call conveniently
    - determine <[p1].sub[<[p0]>].vector_length.add[<[t]>]>

# Procedure script by mcmonkey creator of DModels https://github.com/mcmonkeyprojects/DenizenModels
dcutscene_catmullrom_proc:
    type: procedure
    debug: false
    definitions: p0|p1|p2|p3|t
    script:
    # Zero distances are impossible to calculate
    - if <[p2].sub[<[p1]>].vector_length> < 0.01:
        - determine <[p2]>
    # Based on https://en.wikipedia.org/wiki/Centripetal_Catmull%E2%80%93Rom_spline#Code_example_in_Unreal_C++
    # With safety checks added for impossible situations
    - define t0 0
    - define t1 <proc[dcutscene_catmullrom_get_t].context[0|<[p0]>|<[p1]>]>
    - define t2 <proc[dcutscene_catmullrom_get_t].context[<[t1]>|<[p1]>|<[p2]>]>
    - define t3 <proc[dcutscene_catmullrom_get_t].context[<[t2]>|<[p2]>|<[p3]>]>
    # Divide-by-zero safety check
    - if <[t1].abs> < 0.001 || <[t2].sub[<[t1]>].abs> < 0.001 || <[t2].abs> < 0.001 || <[t3].sub[<[t1]>].abs> < 0.001:
        - determine <[p2].sub[<[p1]>].mul[<[t]>].add[<[p1]>]>
    - define t <[t2].sub[<[t1]>].mul[<[t]>].add[<[t1]>]>
    # ( t1-t )/( t1-t0 )*p0 + ( t-t0 )/( t1-t0 )*p1;
    - define a1 <[p0].mul[<[t1].sub[<[t]>].div[<[t1]>]>].add[<[p1].mul[<[t].div[<[t1]>]>]>]>
    # ( t2-t )/( t2-t1 )*p1 + ( t-t1 )/( t2-t1 )*p2;
    - define a2 <[p1].mul[<[t2].sub[<[t]>].div[<[t2].sub[<[t1]>]>]>].add[<[p2].mul[<[t].sub[<[t1]>].div[<[t2].sub[<[t1]>]>]>]>]>
    # FVector A3 = ( t3-t )/( t3-t2 )*p2 + ( t-t2 )/( t3-t2 )*p3;
    - define a3 <[a1].mul[<[t2].sub[<[t]>].div[<[t2]>]>].add[<[a2].mul[<[t].div[<[t2]>]>]>]>
    # FVector B1 = ( t2-t )/( t2-t0 )*A1 + ( t-t0 )/( t2-t0 )*A2;
    - define b1 <[a1].mul[<[t2].sub[<[t]>].div[<[t2]>]>].add[<[a2].mul[<[t].div[<[t2]>]>]>]>
    # FVector B2 = ( t3-t )/( t3-t1 )*A2 + ( t-t1 )/( t3-t1 )*A3;
    - define b2 <[a2].mul[<[t3].sub[<[t]>].div[<[t3].sub[<[t1]>]>]>].add[<[a3].mul[<[t].sub[<[t1]>].div[<[t3].sub[<[t1]>]>]>]>]>
    # FVector C  = ( t2-t )/( t2-t1 )*B1 + ( t-t1 )/( t2-t1 )*B2;
    - determine <[b1].mul[<[t2].sub[<[t]>].div[<[t2].sub[<[t1]>]>]>].add[<[b2].mul[<[t].sub[<[t1]>].div[<[t2].sub[<[t1]>]>]>]>]>

# Cinematic bars on screen
dcutscene_bars:
    type: task
    debug: false
    definitions: player
    script:
    - define player <[player]||<player>>
    - define script <script[dcutscenes_config].data_key[config]>
    - if <[script.use_cutscene_black_bars]||false>:
      - if <[player].has_flag[dcutscene_bars]>:
        - flag <[player]> dcutscene_bars:!
      - define top <[script.cutscene_black_bar_top]>
      - define bottom <[script.cutscene_black_bar_bottom]>
      - define uuid <util.random_uuid>
      - bossbar create players:<[player]> id:<[uuid]> title:<[top]> color:BLUE
      - flag <[player]> dcutscene_bars:<[uuid]>
      - while <[player].has_flag[dcutscene_bars]> && <[player].is_online>:
        - actionbar <[bottom]> targets:<[player]>
        - wait 1.1s

# Remove cinematic bars from screen
dcutscene_bars_remove:
    type: task
    debug: false
    definitions: player
    script:
    - define player <[player]||<player>>
    - bossbar remove id:<[player].flag[dcutscene_bars]> players:<[player]>
    - flag <[player]> dcutscene_bars:!
    - actionbar <empty> targets:<[player]>