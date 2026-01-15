#===== Denizen Cutscenes GUI======

#######################################################################################
# This contains tasks for handling the GUI and inventory script containers.
#######################################################################################

## GUI Tasks ################
#Show list of cutscenes in a gui
dcutscene_scene_show:
    type: task
    debug: false
    definitions: page
    script:
    - inventory open d:dcutscene_inventory_main
    - define inv <player.open_inventory>
    - if <server.has_flag[dcutscenes]>:
      - if !<player.has_flag[scene_show_page_index]>:
        - flag <player> scene_show_page_index:1
        - define page_index:1
      - else:
        - define page_index <player.flag[scene_show_page_index]>
      - if <[page]||null> == null:
        - define page_index:1
        - flag <player> scene_show_page_index:1
      - else if <[page]> != back:
        - choose <[page]>:
          - case next:
            - flag <player> scene_show_page_index:++
            - define page_index <player.flag[scene_show_page_index]>
          - case previous:
            - flag <player> scene_show_page_index:<[page_index].sub[1].equals[0].if_true[1].if_false[<[page_index].sub[1]>]>
            - define page_index <player.flag[scene_show_page_index]>
      - define max 45
      - define limit <[max].mul[<[page_index]>]>
      - define start <[limit].sub[<[max].sub[1]>]>
      - define data <server.flag[dcutscenes]||<map>>
      - define keys <[data].keys>
      - define size <[keys].size>
      - if <[size]> < <[limit]>:
        - define exceed false
        - define list <[keys].get[<[start]>].to[<[size]>]||<list>>
      - else:
        - define exceed true
        - inventory set d:<player.open_inventory> slot:51 o:dcutscene_next
        - define list <[keys].get[<[start]>].to[<[limit]>]>
      - if <[page_index]> != 1:
        - inventory set d:<player.open_inventory> slot:49 o:dcutscene_previous
      - foreach <[list]> as:scene:
        - define cutscene <[data.<[scene]>]||null>
        - if <[cutscene]> == null:
          - debug error "Scene <[scene]> could not be found."
          - foreach next
        - define desc_color:!
        - define lore1:!
        - define lore:!
        - define world <[cutscene.world]>
        - define settings <[cutscene.settings]||null>
        - define display <blue><bold><[cutscene.display].parse_color||<blue><bold><[cutscene.name]>>
        - define desc <[cutscene.description]||<list>>
        - foreach <[desc]> as:d:
          - define desc_color:->:<[d].parse_color>
        - define item <item[<[settings.item].if_null[dcutscene_scene_item_default]>]>
        - adjust <[item]> display:<[display]> save:item
        - define item <entry[item].result>
        - define lore2 <list[<[desc_color].if_null[<empty>]>]>
        - if <[lore2].any>:
          - define lore1:->:<empty>
          - define lore1:|:<[lore2]>
          - define lore <[lore1]>
          - adjust <[item]> lore:<[lore]> save:item
          - define item <entry[item].result>
        - flag <[item]> cutscene_data:<[cutscene.name]>
        - inventory set d:<[inv]> o:<[item]> slot:<[loop_index]>
      - if !<[exceed].is_truthy>:
        - inventory set d:<[inv]> o:dcutscene_new_scene_item slot:<[list].size.add[1].equals[0].if_true[1].if_false[<[list].size.add[1]>]>
    - else:
      - inventory set d:<[inv]> o:dcutscene_new_scene_item slot:1

# Determine if the keyframe has animators
dcutscene_keyframe_calculate:
    type: procedure
    debug: false
    definitions: scene_name|timespot
    script:
    - define keyframes <server.flag[dcutscenes.<[scene_name]>.keyframes]>
    - define tick_max <duration[<[timespot]>s].in_ticks>
    - define tick_min <[tick_max].sub[9]>
    - define tick_map <map>
    - repeat 9 as:loop_i:
      - define tick <[tick_min].add[<[loop_i]>]>
      #Play Scene Search
      - define play_scene_search <[keyframes.play_scene.tick]||null>
      - if <[play_scene_search].equals[<[tick]>]>:
        - define tick_map.play_scene.tick <[tick]>
        - define tick_map.play_scene.cutscene <[keyframes.play_scene.cutscene]>
      #Stop Search
      - define stop_search <[keyframes.stop.tick]||null>
      - if <[stop_search].equals[<[tick]>]>:
        - define tick_map.stop_point.tick <[tick]>
      #Camera Search
      - define cam_search <[keyframes.camera.<[tick]>]||null>
      - if <[cam_search]> != null:
        - define tick_map.camera.<[tick]> <[cam_search]>
      #Models search
      - define model_search <[keyframes.models.<[tick]>]||null>
      - if <[model_search]> != null:
        - define tick_map.models.<[tick]> <[model_search]>
      #Run Task Search
      - define task_search <[keyframes.elements.run_task.<[tick]>.run_task_list]||null>
      - if <[task_search]> != null:
        - define tick_map.run_task.<[tick]> <[task_search]>
      #Screeneffect Search
      - define screeneffect_search <[keyframes.elements.screeneffect.<[tick]>]||null>
      - if <[screeneffect_search]> != null:
        - define tick_map.screeneffect.<[tick]> <[screeneffect_search]>
      #Sound Search
      - define sound_search <[keyframes.elements.sound.<[tick]>.sounds]||null>
      - if <[sound_search]> != null:
        - define tick_map.sound.<[tick]> <[sound_search]>
      #Fake Block Search
      - define fake_block_search <[keyframes.elements.fake_object.fake_block.<[tick]>.fake_blocks]||null>
      - if <[fake_block_search]> != null:
        - define tick_map.fake_blocks.<[tick]> <[fake_block_search]>
      #Fake Schem Search
      - define fake_schem_search <[keyframes.elements.fake_object.fake_schem.<[tick]>.fake_schems]||null>
      - if <[fake_schem_search]> != null:
        - define tick_map.fake_schems.<[tick]> <[fake_schem_search]>
      #Particle Search
      - define particle_search <[keyframes.elements.particle.<[tick]>.particle_list]||null>
      - if <[particle_search]> != null:
        - define tick_map.particle.<[tick]> <[particle_search]>
      #Title Search
      - define title_search <[keyframes.elements.title.<[tick]>]||null>
      - if <[title_search]> != null:
        - define tick_map.title.<[tick]> <[title_search]>
      #Command Search
      - define command_search <[keyframes.elements.command.<[tick]>.command_list]||null>
      - if <[command_search]> != null:
        - define tick_map.command.<[tick]> <[command_search]>
      #Message Search
      - define message_search <[keyframes.elements.message.<[tick]>.message_list]||null>
      - if <[message_search]> != null:
        - define tick_map.message.<[tick]> <[message_search]>
      #Time Search
      - define time_search <[keyframes.elements.time.<[tick]>]||null>
      - if <[time_search]> != null:
        - define tick_map.time.<[tick]> <[time_search]>
      #Weather Search
      - define weather_search <[keyframes.elements.weather.<[tick]>]||null>
      - if <[weather_search]> != null:
        - define tick_map.weather.<[tick]> <[weather_search]>
    - if !<[tick_map].is_empty>:
      - determine <[tick_map]>
    - else:
      - determine null

# Utilized for showing animators within each sub keyframe as lore
# by gathering the data from the keyframes and then displaying it
dcutscene_keyframe_modify:
    type: task
    debug: false
    definitions: page
    script:
    - define data <player.flag[cutscene_data]>
    - define keyframes <[data.keyframes]>
    #Max adjustable slots in inventory
    - define max 45
    #0.45 = 9 ticks for 9 slots per row
    - if !<player.has_flag[keyframe_modify_index]>:
      - inventory open d:dcutscene_inventory_keyframe
      - flag <player> keyframe_modify_index:1
      - define page_index 1
    - else:
      #Determine page
      - define page <[page]||null>
      - if <[page]> == null:
        - inventory open d:dcutscene_inventory_keyframe
        - define page_index 1
        - flag <player> keyframe_modify_index:<[page_index]>
      - else:
        - define page_index <player.flag[keyframe_modify_index]>
        - choose <[page]>:
          #next page button
          - case next:
            - define page_index <[page_index].add[1]>
            - flag <player> keyframe_modify_index:<[page_index]>
          #previous page button
          - case previous:
            - define page_index <[page_index].sub[1].equals[0].if_true[1].if_false[<[page_index].sub[1]>]>
            - flag <player> keyframe_modify_index:<[page_index]>
          #back page
          - case back:
            - inventory open d:dcutscene_inventory_keyframe
    - define inv <player.open_inventory>
    #time increments
    - define inc 0.45
    #constant increment
    - define new_inc <[inc].mul[45].mul[<[page_index].sub[1]>]>
    - repeat <[max]> as:loop_i:
        - define lore_list:!
        - define stop_point null
        #======= Time Calculations =======
        #page 1 and loop 1
        - if <[loop_i]> == 1 && <[page_index]> == 1:
          - define time <[inc].round_up_to_precision[0.1]>s
          - define timespot <[inc]>
          #format the time should it be greater than 1 minute
          - if <duration[<[time]>].is_more_than_or_equal_to[60s]>:
            - define time <duration[<[time]>].formatted>
          - define display "<blue><bold>Time <gray><bold><[time]>"
        - else:
          #default
          - if <[page_index]> == 1:
            - define inc <[inc].add[0.45]>
            - define timespot <[inc]>
            - define time <duration[<[inc].round_up_to_precision[0.1]>].formatted>
            - if <duration[<[time]>].is_more_than_or_equal_to[60s]>:
              - define time <duration[<[time]>].formatted>
            - define display "<blue><bold>Time <gray><bold><[time]>"
          #calculate new pages
          - else:
            - define inc <[inc].add[0.45]>
            - define timespot <[inc].add[<[new_inc]>]>
            - define time <[inc].add[<[new_inc]>].round_up_to_precision[0.1]>s
            - if <duration[<[time]>].is_more_than_or_equal_to[60s]>:
              - define time <duration[<[time]>].formatted>
            - define display "<blue><bold>Time <gray><bold><[time]>"
        #======= Keyframe data gathering =======
        - define keyframe_data <[data.name].proc[dcutscene_keyframe_calculate].context[<[timespot]>]>
        - if <[keyframe_data].equals[null]>:
          - define lore_list null
        - else:
          #Camera
          - define camera <[keyframe_data.camera]||null>
          - if <[camera]> != null:
            - foreach <[camera]> key:tick as:camera:
              - define text "<aqua>Camera on tick <green><[tick]>t <aqua>at location <green><[camera.location].simple>"
              - define lore_list:->:<[text]>
          #All Model Data
          - define models <[keyframe_data.models]||null>
          - if <[models]> != null:
            #All Models
            - foreach <[models]> key:tick as:model_data:
              #Model list in tick
              - define model_list <[model_data.model_list]>
              - foreach <[model_list]> as:model_uuid:
                #Data of model based on uuid within the tick
                - define uuid_data <[model_data.<[model_uuid]>]>
                - define id <[uuid_data.id]>
                - choose <[uuid_data.type]>:
                  - case model:
                    - define text "<aqua>Model <green><[id]> <aqua>on tick <green><[tick]>t"
                    - define lore_list:->:<[text]>
                  - case player_model:
                    - define text "<aqua>Player Model <green><[id]> <aqua>on tick <green><[tick]>t"
                    - define lore_list:->:<[text]>
          #Run Task
          - define run_task <[keyframe_data.run_task]||null>
          - if <[run_task]> != null:
            - foreach <[run_task]> key:tick as:task_ids:
              - foreach <[task_ids]> as:task_uuid:
                - define task_data <[keyframes.elements.run_task.<[tick]>.<[task_uuid]>]||null>
                - if <[task_data]> != null:
                  - define text "<aqua>Run Task <green><[task_data.script]> <aqua>on tick <green><[tick]>t"
                  - define lore_list:->:<[text]>
          #Screeneffect
          - define screeneffect <[keyframe_data.screeneffect]||null>
          - if <[screeneffect]> != null:
            - foreach <[screeneffect]> key:tick as:screeneffect:
              - define text "<aqua>Cinematic Screeneffect on tick <green><[tick]>t"
              - define lore_list:->:<[text]>
          #Sound
          - define sounds <[keyframe_data.sound]||null>
          - if <[sounds]> != null:
            - foreach <[sounds]> key:tick as:sound_ids:
              - foreach <[sound_ids]> as:sound_uuid:
                - define sound_data <[keyframes.elements.sound.<[tick]>.<[sound_uuid]>]||null>
                - if <[sound_data]> != null:
                  - define text "<aqua>Sound <green><[sound_data.sound]> <aqua>on tick <green><[tick]>t"
                  - define lore_list:->:<[text]>
          #Fake Blocks
          - define fake_blocks <[keyframe_data.fake_blocks]||null>
          - if <[fake_blocks]> != null:
            - foreach <[fake_blocks]> key:tick as:object_ids:
              - foreach <[object_ids]> as:object_uuid:
                - define fake_block <[keyframes.elements.fake_object.fake_block.<[tick]>.<[object_uuid]>]||null>
                - if <[fake_block]> != null:
                  - define block <material[<[fake_block.block]>].name>
                  - define text "<aqua>Fake block <green><[block]> <aqua>on tick <green><[tick]>t"
                  - define lore_list:->:<[text]>
          #Fake Schems
          - define fake_schems <[keyframe_data.fake_schems]||null>
          - if <[fake_schems]> != null:
            - foreach <[fake_schems]> key:tick as:object_ids:
              - foreach <[object_ids]> as:object_uuid:
                - define fake_schem <[keyframes.elements.fake_object.fake_schem.<[tick]>.<[object_uuid]>]||null>
                - if <[fake_schem]> != null:
                  - define schem_name <[fake_schem.schem]>
                  - define text "<aqua>Fake schem <green><[schem_name]> <aqua>on tick <green><[tick]>t"
                  - define lore_list:->:<[text]>
          #Particle
          - define particles <[keyframe_data.particle]||null>
          - if <[particles]> != null:
            - foreach <[particles]> key:tick as:particle_ids:
              - foreach <[particle_ids]> as:particle_uuid:
                - define particle_data <[keyframes.elements.particle.<[tick]>.<[particle_uuid]>]||null>
                - if <[particle_data]> != null:
                  - define text "<aqua>Particle <green><[particle_data.particle]> <aqua>on tick <green><[tick]>t"
                  - define lore_list:->:<[text]>
          #Title
          - define title <[keyframe_data.title]||null>
          - if <[title]> != null:
            - foreach <[title]> key:tick as:title:
              - define text "<aqua>Title <white><[title.title].parse_color> <aqua>on tick <green><[tick]>t"
              - define lore_list:->:<[text]>
          #Command
          - define command <[keyframe_data.command]||null>
          - if <[command]> != null:
            - foreach <[command]> key:tick as:command_ids:
              - foreach <[command_ids]> as:command_uuid:
                - define command_data <[keyframes.elements.command.<[tick]>.<[command_uuid]>]||null>
                - if <[command_data]> != null:
                  - define text "<aqua>Command on tick <green><[tick]>t"
                  - define lore_list:->:<[text]>
          #Message
          - define message <[keyframe_data.message]||null>
          - if <[message]> != null:
            - foreach <[message]> key:tick as:message_ids:
              - foreach <[message_ids]> as:message_uuid:
                - define msg_data <[keyframes.elements.message.<[tick]>.<[message_uuid]>]||null>
                - if <[msg_data]> != null:
                  - define text "<aqua>Message on tick <green><[tick]>t"
                  - define lore_list:->:<[text]>
          #Time
          - define time <[keyframe_data.time]||null>
          - if <[time]> != null:
            - foreach <[time]> key:tick as:time:
              - define text "<aqua>Time <green><duration[<[time.time]>].in_ticks>t <aqua>on tick <green><[tick]>t"
              - define lore_list:->:<[text]>
          #Weather
          - define weather <[keyframe_data.weather]||null>
          - if <[weather]> != null:
            - foreach <[weather]> key:tick as:weather:
              - define text "<aqua>Weather <green><[weather.weather]> <aqua>on tick <green><[tick]>t"
              - define lore_list:->:<[text]>
          #Play Scene
          - define play_scene <[keyframe_data.play_scene]||null>
          - if <[play_scene]> != null:
            - define text "<blue>Play scene <green><[play_scene.cutscene]> <blue>on tick <green><[play_scene.tick]>t"
            - define lore_list:->:<[text]>
          #Stop Point
          - define stop_point <[keyframe_data.stop_point]||null>
          - if <[stop_point]> != null:
            - define stop_tick <[stop_point.tick]>
            - define text "<red>Stop point on tick <green><[stop_tick]>t"
            - define lore_list:->:<[text]>
          #If lore list exceeds lore limit
          - define max_lore_size <script[dcutscenes_config].data_key[config].get[cutscene_main_keyframe_lore_limit]||15>
          - define lore_list <[lore_list]||<list>>
          - if <[lore_list].size> > <[max_lore_size]>:
            - define lore_list <[lore_list].get[1].to[<[max_lore_size]>]>
            - define lore_list:->:<gray>...
        #======= Setting information on items =======
        - if <[lore_list]> != null && <[stop_point]> == null:
          - define item <item[dcutscene_keyframe_contains]>
        - else if <[stop_point]> != null:
          - define item <item[dcutscene_keyframe_stop_point]>
        - else:
          - define item <item[dcutscene_keyframe]>
        - adjust <[item]> display:<[display]> save:item
        - define item <entry[item].result>
        - define click "<gray><italic>Click to modify keyframe"
        - define animators "<blue><bold>Animators:"
        - if <[lore_list]> != null:
          - if <[lore_list].size> == 1:
            - define animators "<blue><bold>Animator:"
          - adjust <[item]> lore:<list[<empty>|<[animators]>|<[lore_list]>|<[click]>].combine> save:item
          - define item <entry[item].result>
        - else:
          - adjust <[item]> lore:<list[<empty>|<[click]>]> save:item
        - define item <entry[item].result>
        - define keyframe.timespot <[timespot]>
        - flag <[item]> keyframe_data:<[keyframe]>
        - inventory set d:<[inv]> o:<[item]> slot:<[loop_i]>

#Sub keyframe list
dcutscene_sub_keyframe_modify:
    type: task
    debug: false
    definitions: keyframe
    script:
    #Data for returning to previous page
    - flag <player> dcutscene_sub_keyframe_back_data:<[keyframe]>
    - define data <player.flag[cutscene_data]>
    - define scene_name <[data.name]>
    - define keyframes <[data.keyframes]>
    - define camera <[keyframes.camera]||null>
    - define models <[keyframes.models]||null>
    - define elements <[keyframes.elements]||null>
    - define origin <[data.settings.origin]||false>
    - define inv <player.open_inventory>
    - define slots <[inv].map_slots>
    #Reset inv
    - repeat 45 as:slot_i:
      - define slot <[slots.<[slot_i]>]||null>
      - if <[slot]> != null:
        - inventory set d:<[inv]> o:air slot:<[slot_i]>
    - define time <[keyframe.timespot]>
    - define tick_max <duration[<[time]>s].in_ticks>
    - define tick_min <[tick_max].sub[9]>
    #Play Scene
    - define play_scene <[keyframes.play_scene]||none>
    #Stop Point
    - define stop_point <[keyframes.stop.tick]||<[tick_max].add[1]>>
    #Used for scrolling down or up
    - define tick_page <player.flag[sub_keyframe_tick_page]>
    - define tick_page_max <[tick_page].add[4]>
    - repeat 9 as:loop_i:
        #Reset for each tick
        - define tick_index:0
        - define tick_row:0
        - define cam_lore:!
        #Tick
        - define tick <[tick_min].add[<[loop_i]>]>
        #Ticks columns
        - define tick_column 9
        #If column contains animators
        - define tick_column_check none
        #=========== Camera check (First Priority)============
        - if <[camera]> != null && <[camera].contains[<[tick]>]>:
          - define tick_column_check true
          - define tick_index:++
          - define tick_row:++
          - if <[tick_row]> > 4:
            - define tick_row:1
          - if <[tick_index]> > <[tick_page]>:
            - define cam_item <item[dcutscene_camera_keyframe]>
            - define cam_data <[camera.<[tick]>]>
            - if <[origin].is_truthy>:
              - define cam_loc "<aqua>Location Offset: <gray><[cam_data.origin_offset]||0,0,0>"
            - else:
              - define cam_loc "<aqua>Location: <gray><location[<[cam_data.location]>].simple>"
            - define cam_eye_loc <[cam_data.eye_loc.boolean]>
            - choose <[cam_eye_loc]>:
              - case true:
                - define cam_look_data <location[<[cam_data.eye_loc.location]>].simple>
              - case false:
                - define cam_look_data false
            - define cam_look "<aqua>Look Location: <gray><[cam_look_data]>"
            - define cam_interp "<aqua>Path Interpolation: <gray><[cam_data.interpolation].to_uppercase>"
            - define cam_rotate "<aqua>Rotate Multiplier: <gray><[cam_data.rotate_mul]||1.0>"
            - define cam_interp_look "<aqua>Interpolate Look: <gray><[cam_data.interpolate_look]||true>"
            - define cam_move "<aqua>Move: <gray><[cam_data.move]||true>"
            - define cam_invert "<aqua>Invert Look: <gray><[cam_data.invert]||false>"
            - if <[cam_data.recording.bool]||false> != false:
              - define cam_recording "<aqua>Recording: <gray>true"
            - else:
              - define cam_recording "<aqua>Recording: <gray>false"
            - define cam_tick "<aqua>Time: <gray><[cam_data.tick]||<[tick]>>t"
            - define modify "<gray><italic>Click to modify camera"
            - define cam_lore <list[<empty>|<[cam_loc]>|<[cam_look]>|<[cam_interp]>|<[cam_rotate]>|<[cam_interp_look]>|<[cam_move]>|<[cam_invert]>|<[cam_recording]>|<[cam_tick]>|<empty>|<[modify]>]>
            - adjust <[cam_item]> lore:<[cam_lore]> save:item
            - define cam_item <entry[item].result>
            - define display <dark_gray><bold>Camera
            - adjust <[cam_item]> display:<[display]> save:item
            - define cam_item <entry[item].result>
            #Data to pass through for use of modifying the camera
            - definemap modify_data type:camera tick:<[tick]>
            - flag <[cam_item]> keyframe_opt_modify:<[modify_data]>
            #-GUI placement calculation for tick row
            - inventory set d:<[inv]> o:<[cam_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
            - define add_item <item[dcutscene_keyframe_tick_add]>
            - flag <[add_item]> keyframe_modify:<[tick]>
            - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_index]>]>].add[9]>
          - else:
            - define add_item <item[dcutscene_keyframe_tick_add]>
            - flag <[add_item]> keyframe_modify:<[tick]>
            - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #========= Model Animators Check ===========
        - define model_data <[models.<[tick]>]||null>
        - if <[model_data]> != null:
          - define tick_column_check true
          - define model_list <[model_data.model_list]||<list>>
          - foreach <[model_list]> as:model_uuid:
            - define tick_index:++
            - define tick_row:++
            - if <[tick_row]> > 4:
              - define tick_row:1
            - if <[tick_index]> > <[tick_page]>:
              - define m_data <[model_data.<[model_uuid]>]>
              #Root model check
              - define root_check <[m_data.root]||none>
              - if <[root_check]> != none:
                - define root_tick <[m_data.root.tick]>
                - define root_uuid <[m_data.root.uuid]>
                - define start_tick <[models.<[root_tick]>.<[root_uuid]>.path].keys.first||null>
                - define lore_data <[models.<[root_tick]>.<[root_uuid]>.path.<[tick]>]>
                - define opt_item <item[<[models.<[root_tick]>.<[root_uuid]>.item]||dcutscene_model_keyframe_item>]>
              - else:
                - define lore_data <[models.<[tick]>.<[model_uuid]>.path.<[tick]>]>
                - define opt_item <item[<[models.<[tick]>.<[model_uuid]>.item]||dcutscene_model_keyframe_item>]>
              - choose <[m_data.type]>:
                #===== Model =====
                - case model:
                  - adjust <[opt_item]> display:<red><bold><[m_data.id]> save:item
                  - define opt_item <entry[item].result>
                  - define time_lore "<aqua>Time: <gray><[tick]>t"
                  - define modify "<gray><italic>Click to modify model"
                  #Data to lore
                  - define type_lore "<aqua>Type: <gray><[m_data.type]>"
                  - define model_lore "<aqua>Model: <gray><[m_data.model].if_null[<[models.<[root_tick]>.<[root_uuid]>.model]>]>"
                  - define anim_lore "<aqua>Animation: <gray><[lore_data.animation]>"
                  - define loc_lore "<aqua>Location: <gray><[lore_data.location].simple>"
                  - define path_interp_lore "<aqua>Path Interpolation: <gray><[lore_data.interpolation]>"
                  - define rotate_interp "<aqua>Rotate Interpolation: <gray><[lore_Data.rotate_interp]>"
                  - define rotate_mul "<aqua>Rotate Multiplier: <gray><[lore_data.rotate_mul]||1.0>"
                  - define ray_dir "<aqua>Ray Trace Direction: <gray><[lore_data.ray_trace.direction]>"
                  - define move_lore "<aqua>Move: <gray><[lore_data.move]>"
                  #=Non root model (sub-frame)
                  - if <[root_check]> != none:
                    - if <[start_tick]> == null:
                      - define start_lore <empty>
                      - define m_lore <list[<empty>|<[type_lore]>|<[model_lore]>|<[time_lore]>|<[anim_lore]>|<[loc_lore]>|<[path_interp_lore]>|<[rotate_interp]>|<[rotate_mul]>|<[ray_dir]>|<[move_lore]>|<empty>|<[modify]>]>
                    - else:
                      - define start_lore "<aqua>Starting Frame: <gray><[start_tick]>t"
                      - define m_lore <list[<empty>|<[type_lore]>|<[model_lore]>|<[time_lore]>|<[anim_lore]>|<[loc_lore]>|<[path_interp_lore]>|<[rotate_interp]>|<[rotate_mul]>|<[ray_dir]>|<[move_lore]>|<[start_lore]>|<empty>|<[modify]>]>
                  #=Root Model
                  - else:
                    #Lore
                    - define sub_frames "<aqua>Sub Frames: <gray><[m_data.sub_frames].keys.size||0>"
                    - define m_lore <list[<empty>|<[type_lore]>|<[model_lore]>|<[time_lore]>|<[anim_lore]>|<[loc_lore]>|<[path_interp_lore]>|<[rotate_interp]>|<[rotate_mul]>|<[ray_dir]>|<[move_lore]>|<[sub_frames]>|<empty>|<[modify]>]>
                  - adjust <[opt_item]> lore:<[m_lore]> save:item
                  - define opt_item <entry[item].result>
                  #Data to pass through for use of modifying the modifier
                  - definemap modify_data type:model tick:<[tick]> uuid:<[model_uuid]>
                  - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>

                #===== Player Model =====
                - case player_model:
                  - define opt_item <item[dcutscene_keyframe_player_model]>
                  - adjust <[opt_item]> display:<blue><bold><[m_data.id]> save:item
                  - define opt_item <entry[item].result>
                  - define time_lore "<aqua>Time: <gray><[tick]>t"
                  - define modify "<gray><italic>Click to modify player model"
                  #Data to lore
                  - define type_lore "<aqua>Type: <gray><[m_data.type].replace[_].with[ ]>"
                  - define anim_lore "<aqua>Animation: <gray><[lore_data.animation]>"
                  - define loc_lore "<aqua>Location: <gray><[lore_data.location].simple>"
                  - define path_interp_lore "<aqua>Path Interpolation: <gray><[lore_data.interpolation]>"
                  - define rotate_interp "<aqua>Rotate Interpolation: <gray><[lore_data.rotate_interp]>"
                  - define rotate_mul "<aqua>Rotate Multiplier: <gray><[lore_data.rotate_mul]||1.0>"
                  - define ray_dir "<aqua>Ray Trace Direction: <gray><[lore_data.ray_trace.direction]>"
                  - define move_lore "<aqua>Move: <gray><[lore_data.move]>"
                  #Skin
                  - define skin <proc[dcutscene_determine_player_model_skin].context[<[scene_name]>|<[tick]>|<[model_uuid]>]||none>
                  #=Non root model (sub-frame)
                  - if <[root_check]> != none:
                    #Lore information
                    - if <[skin]> == none || <[skin]> == player:
                      - define skin <player>
                    #If the starting point keyframe has a skin update the ones without a skin
                    - adjust <[opt_item]> skull_skin:<[skin].parsed.skull_skin||<player.skull_skin>> save:item
                    - define opt_item <entry[item].result>
                    - define skin_lore "<aqua>Skin: <gray><[skin].parsed.name||<[skin].name>>"
                    #Lore
                    - if <[start_tick]> == null:
                      - define start_lore <empty>
                      - define m_lore <list[<empty>|<[type_lore]>|<[time_lore]>|<[anim_lore]>|<[skin_lore]>|<[loc_lore]>|<[path_interp_lore]>|<[rotate_interp]>|<[rotate_mul]>|<[ray_dir]>|<[move_lore]>|<empty>|<[modify]>]>
                    - else:
                      - define start_lore "<aqua>Starting Frame: <gray><[start_tick]>t"
                      - define m_lore <list[<empty>|<[type_lore]>|<[time_lore]>|<[anim_lore]>|<[skin_lore]>|<[loc_lore]>|<[path_interp_lore]>|<[rotate_interp]>|<[rotate_mul]>|<[ray_dir]>|<[move_lore]>|<[start_lore]>|<empty>|<[modify]>]>
                  #=Root Model
                  - else:
                    - if <[skin]> == none || <[skin]> == player:
                      - define skin <player>
                    - adjust <[opt_item]> skull_skin:<[skin].parsed.skull_skin||<player.skull_skin>> save:item
                    - define opt_item <entry[item].result>
                    - define skin_lore "<aqua>Skin: <gray><[skin].parsed.name||<[skin].name>>"
                    #Lore
                    - define sub_frames "<aqua>Sub Frames: <gray><[m_data.sub_frames].keys.size||0>"
                    - define m_lore <list[<empty>|<[type_lore]>|<[time_lore]>|<[anim_lore]>|<[skin_lore]>|<[loc_lore]>|<[path_interp_lore]>|<[rotate_interp]>|<[rotate_mul]>|<[ray_dir]>|<[move_lore]>|<[sub_frames]>|<empty>|<[modify]>]>
                  - adjust <[opt_item]> lore:<[m_lore]> save:item
                  - define opt_item <entry[item].result>
                  #Data to pass through for use of modifying the modifier
                  - definemap modify_data type:player_model tick:<[tick]> uuid:<[model_uuid]>
                  - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>

              #-GUI placement calculation for tick row
              - if <[tick_index]> <= <[tick_page_max]>:
                - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
                - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
                - if <[add_slot]> < 46:
                  - define add_item <item[dcutscene_keyframe_tick_add]>
                  - flag <[add_item]> keyframe_modify:<[tick]>
                  - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
            - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #======== Regular Animators check =========

        #========= Run Task =========
        - define run_task <[elements.run_task.<[tick]>]||null>
        - if <[run_task]> != null:
          - define tick_column_check true
          - define opt_item <item[dcutscene_run_task_keyframe]>
          - define run_task_list <[elements.run_task.<[tick]>.run_task_list]||<list>>
          - foreach <[run_task_list]> as:task_id:
            - define tick_index:++
            - define tick_row:++
            - if <[tick_row]> > 4:
              - define tick_row:1
            - if <[tick_index]> > <[tick_page]>:
              - define data <[elements.run_task.<[tick]>.<[task_id]>]>
              - define task_name "<aqua>Script: <gray><[data.script]>"
              - define task_defs "<aqua>Definitions: <gray><[data.defs]>"
              - define task_wait "<aqua>Waitable: <gray><[data.waitable]>"
              - define task_delay "<aqua>Delay: <gray><duration[<[data.delay]>].in_seconds>s"
              - define modify "<gray><italic>Click to modify run task"
              - define task_lore <list[<empty>|<[task_name]>|<[task_defs]>|<[task_wait]>|<[task_delay]>|<empty>|<[modify]>]>
              - adjust <[opt_item]> lore:<[task_lore]> save:item
              - define opt_item <entry[item].result>
              #Data to pass through for use of modifying the animator
              - definemap modify_data type:run_task tick:<[tick]> uuid:<[task_id]>
              - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
              #-GUI placement calculation for tick row
              - if <[tick_index]> <= <[tick_page_max]>:
                - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
                - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
                - if <[add_slot]> < 46:
                  - define add_item <item[dcutscene_keyframe_tick_add]>
                  - flag <[add_item]> keyframe_modify:<[tick]>
                  - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
            - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #========== Fake Objects =========

        #==== Fake Schematic ====
        - define fake_schem_check <[elements.fake_object.fake_schem.<[tick]>]||null>
        - if <[fake_schem_check]> != null:
          - define tick_column_check true
          - define opt_item <item[dcutscene_fake_schem_keyframe]>
          - define fake_schems_list <[elements.fake_object.fake_schem.<[tick]>.fake_schems]||<list>>
          - foreach <[fake_schems_list]> as:object_id:
            - define tick_index:++
            - define tick_row:++
            - if <[tick_row]> > 4:
              - define tick_row:1
            - if <[tick_index]> > <[tick_page]>:
              - define obj_data <[elements.fake_object.fake_schem.<[tick]>.<[object_id]>]>
              - define l1 "<aqua>Name: <gray><[obj_data.schem]>"
              - define l2 "<aqua>Location: <gray><[obj_data.loc].simple>"
              - define l3 "<aqua>Duration: <gray><duration[<[obj_data.duration]>].formatted>"
              - define l4 "<aqua>Noair: <gray><[obj_data.noair]>"
              - define l5 "<aqua>Waitable: <gray><[obj_data.waitable]>"
              - define l6 "<aqua>Direction: <gray><[obj_data.angle]>"
              - define modify "<gray><italic>Click to modify fake schematic"
              - define schem_lore <list[<empty>|<[l1]>|<[l2]>|<[l3]>|<[l4]>|<[l5]>|<[l6]>|<empty>|<[modify]>]>
              - adjust <[opt_item]> lore:<[schem_lore]> save:item
              - define opt_item <entry[item].result>
              #Data to pass through for use of modifying the animator
              - definemap modify_data type:fake_schem tick:<[tick]> uuid:<[object_id]>
              - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
              #-GUI placement calculation for tick row
              - if <[tick_index]> <= <[tick_page_max]>:
                - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
                - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
                - if <[add_slot]> < 46:
                  - define add_item <item[dcutscene_keyframe_tick_add]>
                  - flag <[add_item]> keyframe_modify:<[tick]>
                  - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
            - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #==== Fake Block ====
        - define fake_block_check <[elements.fake_object.fake_block.<[tick]>]||null>
        - if <[fake_block_check]> != null:
          - define tick_column_check true
          - define opt_item <item[dcutscene_fake_block_keyframe]>
          - define fake_blocks_list <[elements.fake_object.fake_block.<[tick]>.fake_blocks]||<list>>
          - foreach <[fake_blocks_list]> as:object_id:
            - define tick_index:++
            - define tick_row:++
            - if <[tick_row]> > 4:
              - define tick_row:1
            - if <[tick_index]> > <[tick_page]>:
              - define obj_data <[elements.fake_object.fake_block.<[tick]>.<[object_id]>]>
              - define block_mat <[obj_data.block]>
              - define l1 "<aqua>Material: <gray><[block_mat].name>"
              - adjust <[opt_item]> material:<material[<[block_mat]>]> save:item
              - define opt_item <entry[item].result>
              - define l2 "<aqua>Location: <gray><[obj_data.loc].simple>"
              - define l3 "<aqua>Duration: <gray><duration[<[obj_data.duration]>].formatted>"
              - define l4 "<aqua>Procedure: <gray><[obj_data.procedure.script]||none>"
              - define l5 "<aqua>Procedure Definition: <gray><[obj_data.procedure.defs]||none>"
              - define modify "<gray><italic>Click to modify fake block"
              - define fake_obj_lore <list[<empty>|<[l1]>|<[l2]>|<[l3]>|<[l4]>|<[l5]>|<empty>|<[modify]>]>
              - adjust <[opt_item]> lore:<[fake_obj_lore]> save:item
              - define opt_item <entry[item].result>
              #Data to pass through for use of modifying the animator
              - definemap modify_data type:fake_block tick:<[tick]> uuid:<[object_id]>
              - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
              #-GUI placement calculation for tick row
              - if <[tick_index]> <= <[tick_page_max]>:
                - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
                - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
                - if <[add_slot]> < 46:
                  - define add_item <item[dcutscene_keyframe_tick_add]>
                  - flag <[add_item]> keyframe_modify:<[tick]>
                  - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
            - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #========= Particle ===========
        - define particle <[elements.particle.<[tick]>]||null>
        - if <[particle]> != null:
          - define tick_column_check true
          - define opt_item <item[dcutscene_particle_keyframe]>
          - define particle_list <[elements.particle.<[tick]>.particle_list]||<list>>
          - foreach <[particle_list]> as:particle_id:
            - define tick_index:++
            - define tick_row:++
            - if <[tick_row]> > 4:
              - define tick_row:1
            - if <[tick_index]> > <[tick_page]>:
              - define p_data <[elements.particle.<[tick]>.<[particle_id]>]>
              - define modify "<gray><italic>Click to modify particle"
              - define l1 "<aqua>Particle: <gray><[p_data.particle]>"
              - define l2 "<aqua>Location: <gray><[p_data.loc].simple>"
              - define l3 "<aqua>Quantity: <gray><[p_data.quantity]>"
              - define l4 "<aqua>Range: <gray><[p_data.range]>"
              - define l5 "<aqua>Repeat: <gray><[p_data.repeat]>"
              - define l6 "<aqua>Repeat Interval: <gray><duration[<[p_data.repeat_interval]>].formatted||0>"
              - define l7 "<aqua>Offset: <gray><[p_data.offset]>"
              - define l8 "<aqua>Procedure: <gray><[p_data.procedure.script]>"
              - define l9 "<aqua>Procedure Definition: <gray><[p_data.procedure.defs]>"
              - define l10 "<aqua>Special Data: <gray><[p_data.special_data]>"
              - define l11 "<aqua>Velocity: <gray><[p_data.velocity]>"
              - define lore <list[<empty>|<[l1]>|<[l2]>|<[l3]>|<[l4]>|<[l5]>|<[l6]>|<[l7]>|<[l8]>|<[l9]>|<[l10]>|<[l11]>|<empty>|<[modify]>]>
              - adjust <[opt_item]> lore:<[lore]> save:item
              - define opt_item <entry[item].result>
              #Data to pass through for use of modifying the animator
              - definemap modify_data type:particle tick:<[tick]> uuid:<[particle_id]>
              - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
              #-GUI placement calculation for tick row
              - if <[tick_index]> <= <[tick_page_max]>:
                - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
                - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
                - if <[add_slot]> < 46:
                  - define add_item <item[dcutscene_keyframe_tick_add]>
                  - flag <[add_item]> keyframe_modify:<[tick]>
                  - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
            - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #========== Screeneffect ==========
        - define screeneffect <[elements.screeneffect.<[tick]>]||null>
        - if <[screeneffect]> != null:
          - define tick_column_check true
          - define opt_item <item[dcutscene_screeneffect_keyframe]>
          - define tick_index:++
          - define tick_row:++
          #Only 4 rows
          - if <[tick_row]> > 4:
            - define tick_row:1
          - if <[tick_index]> > <[tick_page]>:
            - define l1 "<aqua>Fade in: <gray><[screeneffect.fade_in].in_seconds||0>s"
            - define l2 "<aqua>Stay: <gray><[screeneffect.stay].in_seconds||0>s"
            - define l3 "<aqua>Fade out: <gray><[screeneffect.fade_out].in_seconds||0>s"
            - define l4 "<aqua>Color: <gray><[screeneffect.color]||black>"
            - define l5 "<aqua>Time: <gray><[tick]>t"
            - define modify "<gray><italic>Click to modify screeneffect"
            - define effect_lore <list[<empty>|<[l1]>|<[l2]>|<[l3]>|<[l4]>|<[l5]>|<empty>|<[modify]>]>
            - adjust <[opt_item]> lore:<[effect_lore]> save:item
            - define opt_item <entry[item].result>
            #Data to pass through for use of modifying the animator
            - definemap modify_data type:screeneffect tick:<[tick]>
            - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
            #-GUI placement calculation for tick row
            - if <[tick_index]> <= <[tick_page_max]>:
              - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
              - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
              - if <[add_slot]> < 46:
                - define add_item <item[dcutscene_keyframe_tick_add]>
                - flag <[add_item]> keyframe_modify:<[tick]>
                - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
          - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #============ Sound =============
        - define sound <[elements.sound.<[tick]>]||null>
        - if <[sound]> != null:
          - define tick_column_check true
          #Option Item
          - define opt_item <item[dcutscene_sound_keyframe]>
          - define sound_list <[elements.sound.<[tick]>.sounds]||<list>>
          #Gather data from sound list
          - foreach <[sound_list]> as:sound_id:
            - define tick_index:++
            - define tick_row:++
            - if <[tick_row]> > 4:
              - define tick_row:1
            - if <[tick_index]> > <[tick_page]>:
              - define sound_data <[elements.sound.<[tick]>.<[sound_id]>]>
              - define l1 "<aqua>Sound: <gray><[sound_data.sound]>"
              - define l2 "<aqua>Location: <gray><[sound_data.location].simple||false>"
              - define l3 "<aqua>Volume: <gray><[sound_data.volume]>"
              - define l4 "<aqua>Pitch: <gray><[sound_data.pitch]>"
              - define l5 "<aqua>Custom: <gray><[sound_data.custom]>"
              - define l6 "<aqua>Time: <gray><[tick]>t"
              - define modify "<gray><italic>Click to modify sound"
              - define sound_lore <list[<empty>|<[l1]>|<[l2]>|<[l3]>|<[l4]>|<[l5]>|<[l6]>|<empty>|<[modify]>]>
              - adjust <[opt_item]> lore:<[sound_lore]> save:item
              - define opt_item <entry[item].result>
              #Data to pass through for use of modifying the animator
              - definemap modify_data type:sound tick:<[tick]> uuid:<[sound_id]>
              - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
              #-GUI placement calculation for tick row
              - if <[tick_index]> <= <[tick_page_max]>:
                - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
                - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
                - if <[add_slot]> < 46:
                  - define add_item <item[dcutscene_keyframe_tick_add]>
                  - flag <[add_item]> keyframe_modify:<[tick]>
                  - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
            - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #======== Title ==========
        - define title <[elements.title.<[tick]>]||null>
        - if <[title]> != null:
          - define tick_column_check true
          - define opt_item <item[dcutscene_title_keyframe]>
          - define tick_index:++
          - define tick_row:++
          #Only 4 rows
          - if <[tick_row]> > 4:
            - define tick_row:1
          - if <[tick_index]> > <[tick_page]>:
            - define l1 "<aqua>Title: <white><[title.title].parse_color>"
            - define l2 "<aqua>Subtitle: <white><[title.subtitle].parse_color>"
            - define l3 "<aqua>Fade in: <gray><[title.fade_in].in_seconds||0>s"
            - define l4 "<aqua>Stay: <gray><[title.stay].in_seconds||0>s"
            - define l5 "<aqua>Fade out: <gray><[title.fade_out].in_seconds||0>s"
            - define modify "<gray><italic>Click to modify title"
            - define title_lore <list[<empty>|<[l1]>|<[l2]>|<[l3]>|<[l4]>|<[l5]>|<empty>|<[modify]>]>
            - adjust <[opt_item]> lore:<[title_lore]> save:item
            - define opt_item <entry[item].result>
            #Data to pass through for use of modifying the animator
            - definemap modify_data type:title tick:<[tick]>
            - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
            #-GUI placement calculation for tick row
            - if <[tick_index]> <= <[tick_page_max]>:
              - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
              - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
              - if <[add_slot]> < 46:
                - define add_item <item[dcutscene_keyframe_tick_add]>
                - flag <[add_item]> keyframe_modify:<[tick]>
                - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
          - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #========= Command ===========
        - define command <[elements.command.<[tick]>]||null>
        - if <[command]> != null:
          - define tick_column_check true
          #Option Item
          - define opt_item <item[dcutscene_command_keyframe]>
          - define command_list <[elements.command.<[tick]>.command_list]||<list>>
          - foreach <[command_list]> as:command_id:
            - define tick_index:++
            - define tick_row:++
            - if <[tick_row]> > 4:
              - define tick_row:1
            - if <[tick_index]> > <[tick_page]>:
              - define cmd_data <[elements.command.<[tick]>.<[command_id]>]>
              - define l1 "<aqua>Command: <gray><[cmd_data.command]>"
              - define l2 "<aqua>Execute as: <gray><[cmd_data.execute_as]>"
              - define l3 "<aqua>Silent <gray><[cmd_data.silent]>"
              - define modify "<gray><italic>Click to modify command"
              - define command_lore <list[<empty>|<[l1]>|<[l2]>|<[l3]>|<empty>|<[modify]>]>
              - adjust <[opt_item]> lore:<[command_lore]> save:item
              - define opt_item <entry[item].result>
              #Data to pass through for use of modifying the animator
              - definemap modify_data type:command tick:<[tick]> uuid:<[command_id]>
              - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
              #-GUI placement calculation for tick row
              - if <[tick_index]> <= <[tick_page_max]>:
                - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
                - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
                - if <[add_slot]> < 46:
                  - define add_item <item[dcutscene_keyframe_tick_add]>
                  - flag <[add_item]> keyframe_modify:<[tick]>
                  - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
            - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #======== Message =========
        - define message <[elements.message.<[tick]>]||null>
        - if <[message]> != null:
          - define tick_column_check true
          #Option Item
          - define opt_item <item[dcutscene_message_keyframe]>
          - define message_list <[elements.message.<[tick]>.message_list]||<list>>
          - foreach <[message_list]> as:msg_id:
            - define tick_index:++
            - define tick_row:++
            - if <[tick_row]> > 4:
              - define tick_row:1
            - if <[tick_index]> > <[tick_page]>:
              - define msg_data <[elements.message.<[tick]>.<[msg_id]>]>
              - define l1 "<aqua>Message: <white><[msg_data.message].parse_color>"
              - define modify "<gray><italic>Click to modify message"
              - define msg_lore <list[<empty>|<[l1]>|<empty>|<[modify]>]>
              - adjust <[opt_item]> lore:<[msg_lore]> save:item
              - define opt_item <entry[item].result>
              #Data to pass through for use of modifying the animator
              - definemap modify_data type:message tick:<[tick]> uuid:<[msg_id]>
              - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
              #-GUI placement calculation for tick row
              - if <[tick_index]> <= <[tick_page_max]>:
                - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
                - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
                - if <[add_slot]> < 46:
                  - define add_item <item[dcutscene_keyframe_tick_add]>
                  - flag <[add_item]> keyframe_modify:<[tick]>
                  - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
            - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #======= Time =========
        - define time_anim <[elements.time.<[tick]>]||null>
        - if <[time_anim]> != null:
          - define tick_column_check true
          #Option Item
          - define opt_item <item[dcutscene_time_keyframe]>
          - define tick_index:++
          - define tick_row:++
          #Only 4 rows
          - if <[tick_row]> > 4:
            - define tick_row:1
          - if <[tick_index]> > <[tick_page]>:
            - define l1 "<aqua>Time: <gray><duration[<[time_anim.time]>].in_ticks>t"
            - define l2 "<aqua>Duration: <gray><duration[<[time_anim.duration]>].formatted>"
            - define l3 "<aqua>Freeze: <gray><[time_anim.freeze]>"
            - define l4 "<aqua>Reset: <gray><[time_anim.reset]>"
            - define modify "<gray><italic>Click to modify time"
            - define time_lore <list[<empty>|<[l1]>|<[l2]>|<[l3]>|<[l4]>|<empty>|<[modify]>]>
            - adjust <[opt_item]> lore:<[time_lore]> save:item
            - define opt_item <entry[item].result>
            #Data to pass through for use of modifying the animator
            - definemap modify_data type:time tick:<[tick]>
            - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
            #-GUI placement calculation for tick row
            - if <[tick_index]> <= <[tick_page_max]>:
              - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
              - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
              - if <[add_slot]> < 46:
                - define add_item <item[dcutscene_keyframe_tick_add]>
                - flag <[add_item]> keyframe_modify:<[tick]>
                - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
          - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #======= Weather ========
        - define weather <[elements.weather.<[tick]>]||null>
        - if <[weather]> != null:
          - define tick_column_check true
          #Option Item
          - define opt_item <item[dcutscene_weather_keyframe]>
          - define tick_index:++
          - define tick_row:++
          #Only 4 rows
          - if <[tick_row]> > 4:
            - define tick_row:1
          - if <[tick_index]> > <[tick_page]>:
            - define l1 "<aqua>Weather: <gray><[weather.weather]>"
            - define l2 "<aqua>Duration: <gray><duration[<[weather.duration]>].formatted>"
            - define modify "<gray><italic>Click to modify weather"
            - define time_lore <list[<empty>|<[l1]>|<[l2]>|<empty>|<[modify]>]>
            - adjust <[opt_item]> lore:<[time_lore]> save:item
            - define opt_item <entry[item].result>
            #Data to pass through for use of modifying the animator
            - definemap modify_data type:weather tick:<[tick]>
            - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
            #-GUI placement calculation for tick row
            - if <[tick_index]> <= <[tick_page_max]>:
              - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
              - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
              - if <[add_slot]> < 46:
                - define add_item <item[dcutscene_keyframe_tick_add]>
                - flag <[add_item]> keyframe_modify:<[tick]>
                - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
          - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #======== Play Scene =========
        - if <[play_scene]> != none:
          - if <[play_scene.tick].equals[<[tick]>]>:
            - define tick_column_check true
            - define opt_item <item[dcutscene_play_scene_keyframe]>
            - define tick_index:++
            - define tick_row:++
            #Only 4 rows
            - if <[tick_row]> > 4:
              - define tick_row:1
            - if <[tick_index]> > <[tick_page]>:
              - define display "<blue><bold>Play scene <green><bold><[play_scene.cutscene]>"
              - adjust <[opt_item]> display:<[display]> save:item
              - define opt_item <entry[item].result>
              - define l1 "<blue>Cutscene <green><[play_scene.cutscene]> <blue>will play here."
              - define modify "<gray><italic>Click to modify play scene"
              - define play_lore <list[<empty>|<[l1]>|<empty>|<[modify]>]>
              - adjust <[opt_item]> lore:<[play_lore]> save:item
              - define opt_item <entry[item].result>
              #Data to pass through for use of modifying the animator
              - definemap modify_data type:play_scene tick:<[tick]>
              - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
              #-GUI placement calculation for tick row
              - if <[tick_index]> <= <[tick_page_max]>:
                - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
                - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
                - if <[add_slot]> < 46:
                  - define add_item <item[dcutscene_keyframe_tick_add]>
                  - flag <[add_item]> keyframe_modify:<[tick]>
                  - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
            - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #======== Stop Point =========
        - if <[stop_point].equals[<[tick]>]>:
          - define stop_point_check true
          - define tick_column_check true
          - define opt_item <item[dcutscene_stop_scene_keyframe_item]>
          - define tick_index:++
          - define tick_row:++
          #Only 4 rows
          - if <[tick_row]> > 4:
            - define tick_row:1
          - if <[tick_index]> > <[tick_page]>:
            - define l_1 "<red>The cutscene will stop here."
            - define modify "<gray><italic>Click to modify stop point"
            - define stop_lore <list[<empty>|<[l_1]>|<empty>|<[modify]>]>
            - adjust <[opt_item]> lore:<[stop_lore]> save:item
            - define opt_item <entry[item].result>
            #Data to pass through for use of modifying the animator
            - definemap modify_data type:stop_point tick:<[tick]>
            - flag <[opt_item]> keyframe_opt_modify:<[modify_data]>
            #-GUI placement calculation for tick row
            - if <[tick_index]> <= <[tick_page_max]>:
              - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_row]>]>]>
              - define add_slot <[loop_i].add[<[tick_column].mul[<[tick_row]>]>].add[9]>
              - if <[add_slot]> < 46:
                - define add_item <item[dcutscene_keyframe_tick_add]>
                - flag <[add_item]> keyframe_modify:<[tick]>
                - inventory set d:<[inv]> o:<[add_item]> slot:<[add_slot]>
          - else:
              - define add_item <item[dcutscene_keyframe_tick_add]>
              - flag <[add_item]> keyframe_modify:<[tick]>
              - inventory set d:<[inv]> o:<[add_item]> slot:<[loop_i].add[<[tick_column]>]>

        #======= No Animator ========
        - else if <[tick_column_check]> == none && <[tick]> < <[stop_point]>:
          - define opt_item <item[dcutscene_keyframe_tick_add]>
          - flag <[opt_item]> keyframe_modify:<[tick]>
          - define tick_index:++
          - inventory set d:<[inv]> o:<[opt_item]> slot:<[loop_i].add[<[tick_column].mul[<[tick_index]>]>]>
        #If a tick contains 4 or more animators add the up and down buttons
        - if <[tick_index]> >= 4:
          - inventory set d:<[inv]> o:<item[dcutscene_scroll_down]> slot:51
          - inventory set d:<[inv]> o:<item[dcutscene_scroll_up]> slot:49
        #Tick info item
        - define item <item[dcutscene_sub_keyframe]>
        - flag <[item]> keyframe_tick:<[tick]>
        - define display "<aqua><bold>Tick <gray><bold><[tick]>t"
        - adjust <[item]> display:<[display]> save:item
        - define item <entry[item].result>
        - define t_lore "<blue><bold>Main Keyframe Time <gray><bold><duration[<[time]>].formatted>"
        - define s_lore "<green><bold>Tick to seconds <gray><bold><duration[<[tick]>t].in_seconds.round_down_to_precision[0.05]>s"
        #If player is moving or duplicating an animator modify the lore
        - if <player.has_flag[dcutscene_animator_change]>:
          - flag <[item]> change_animator
          - define anim_data <player.flag[dcutscene_animator_change]>
          - if <[anim_data.scene]> == <[data.name]>:
            - choose <[anim_data.type]>:
              - case move:
                - define modify "<gray><italic>Click to move <green><[anim_data.animator].replace[_].with[ ]> <gray><italic>from tick <green><[anim_data.tick]>t <gray><italic>here"
              - case duplicate:
                - define modify "<gray><italic>Click to duplicate <green><[anim_data.animator].replace[_].with[ ]> <gray><italic>from tick <green><[anim_data.tick]>t <gray><italic>here"
            - define lore <list[<[t_lore]>|<[s_lore]>|<empty>|<[modify]>]>
          - else:
            - define lore <list[<[t_lore]>|<[s_lore]>]>
        - else:
          - define lore <list[<[t_lore]>|<[s_lore]>]>
        - adjust <[item]> lore:<[lore]> save:item
        - define item <entry[item].result>
        - inventory set d:<[inv]> o:<[item]> slot:<[loop_i]>

#############################

##Cutscene Inventories (In case your wondering why so many inventories it's just easier to manage)###################
#Main dcutscene gui
dcutscene_inventory_main:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] [dcutscene_exit]

#Cutscene settings GUI
dcutscene_inventory_settings:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [dcutscene_change_scene_name] [dcutscene_change_description_item] [dcutscene_change_show_bars] [dcutscene_duplicate_scene] [dcutscene_change_item] [] []
    - [] [] [dcutscene_hide_players] [dcutscene_bound_to_camera] [dcutscene_save_file_item] [dcutscene_set_origin_point] [dcutscene_origin_point_refresh] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_delete_cutscene] [] [] [] [dcutscene_exit]

#Gui for cutscene scene where you can modify settings and keyframes of the scene
dcutscene_inventory_scene:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [dcutscene_keyframes_list] [] [] [] [dcutscene_settings] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_play_cutscene_item] [] [] [] [dcutscene_exit]

#Scene keyframes
dcutscene_inventory_keyframe:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [dcutscene_previous] [] [dcutscene_next] [] [] [dcutscene_exit]

#Sub keyframe inventory
dcutscene_inventory_sub_keyframe:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [] [] [] [] [dcutscene_exit]

#Modify gui for a sub-keyframe tick
dcutscene_inventory_keyframe_modify:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [dcutscene_add_cam] [dcutscene_add_model] [dcutscene_add_player_model] [dcutscene_add_entity] [dcutscene_add_run_task] [] []
    - [] [] [dcutscene_add_fake_block] [dcutscene_add_fake_schem] [dcutscene_add_screeneffect] [dcutscene_add_particle] [dcutscene_send_title] [] []
    - [] [] [dcutscene_play_command] [dcutscene_add_msg] [dcutscene_add_sound] [dcutscene_send_time] [dcutscene_set_weather] [] []
    - [] [] [dcutscene_play_scene] [dcutscene_stop_scene] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [] [] [] [] [dcutscene_exit]

#Camera GUI
dcutscene_inventory_keyframe_modify_camera:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [dcutscene_camera_loc_modify] [dcutscene_camera_look_modify] [dcutscene_camera_move_modify] [dcutscene_camera_rotate_modify] [dcutscene_camera_interpolate_look] [] []
    - [] [] [dcutscene_camera_upside_down] [dcutscene_camera_interp_modify] [dcutscene_camera_record_player] [dcutscene_camera_path_show] [dcutscene_camera_move_to_keyframe] [] []
    - [] [] [dcutscene_camera_duplicate_to_keyframe] [dcutscene_camera_timespot_play] [dcutscene_camera_teleport] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_camera_remove_modify] [] [] [] [dcutscene_exit]

#Location Tool Modify GUI
dcutscene_inventory_location_tool:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_location_tool_ray_trace_item] [] [dcutscene_location_tool_item] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [dcutscene_location_tool_confirm_location] [] [] [] [dcutscene_exit]

#Model List (If there are previously created models this GUI will appear)
dcutscene_inventory_keyframe_model_list:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_add_new_model] [] [] [] [dcutscene_exit]

#Denizen Models modifier GUI
dcutscene_inventory_keyframe_modify_model:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [dcutscene_model_change_id] [dcutscene_model_change_item] [dcutscene_model_change_location] [dcutscene_model_ray_trace_change] [dcutscene_model_change_model] [] []
    - [] [] [dcutscene_model_change_move] [dcutscene_model_change_animation] [dcutscene_model_interp_method] [dcutscene_model_show_path] [dcutscene_model_interp_rotate_change] [] []
    - [] [] [dcutscene_model_interp_rotate_mul] [dcutscene_model_move_to_keyframe] [dcutscene_model_duplicate] [dcutscene_model_timespot_play] [dcutscene_model_teleport_loc] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [dcutscene_remove_model_tick] [] [dcutscene_remove_model] [] [] [dcutscene_exit]

#Player Model modifier GUI
dcutscene_inventory_keyframe_modify_player_model:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [dcutscene_player_model_change_id] [dcutscene_player_model_change_location] [dcutscene_player_model_ray_trace_change] [dcutscene_player_model_change_move] [dcutscene_player_model_change_animation] [] []
    - [] [] [dcutscene_player_model_change_skin] [dcutscene_player_model_interp_method] [dcutscene_player_model_show_path] [dcutscene_player_model_interp_rotate_change] [dcutscene_player_model_interp_rotate_mul] [] []
    - [] [] [dcutscene_player_model_move_to_keyframe] [dcutscene_player_model_duplicate] [dcutscene_player_model_timespot_play] [dcutscene_player_model_teleport_loc] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [dcutscene_remove_player_model_tick] [] [dcutscene_remove_player_model] [] [] [dcutscene_exit]

#Ray Trace modification for Denizen Models.
dcutscene_inventory_keyframe_ray_trace_model:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_model_ray_trace_determine] [dcutscene_model_ray_trace_liquid] [dcutscene_model_ray_trace_passable] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [] [] [] [] [dcutscene_exit]

#Ray Trace modification for the player model.
dcutscene_inventory_keyframe_ray_trace_player_model:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_player_model_ray_trace_determine] [dcutscene_player_model_ray_trace_liquid] [dcutscene_player_model_ray_trace_passable] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [] [] [] [] [dcutscene_exit]

#Particle modify GUI
dcutscene_inventory_particle_modify:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [dcutscene_particle_modify] [dcutscene_particle_loc_modify] [dcutscene_particle_quantity_modify] [dcutscene_particle_range_modify] [dcutscene_particle_repeat_modify] [] []
    - [] [] [dcutscene_particle_repeat_interval_modify] [dcutscene_particle_offset_modify] [dcutscene_particle_procedure_modify] [dcutscene_particle_procedure_defs_modify] [dcutscene_particle_special_data_modify] [] []
    - [] [] [dcutscene_particle_velocity_modify] [dcutscene_particle_move_to_keyframe] [dcutscene_particle_duplicate] [dcutscene_particle_timespot_play] [dcutscene_particle_teleport_to] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_particle_remove] [] [] [] [dcutscene_exit]

#Fake block modify GUI
dcutscene_inventory_fake_object_block_modify:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_fake_object_block_loc_change] [dcutscene_fake_object_block_material_change] [dcutscene_fake_object_block_duration_change] [] [] []
    - [] [] [] [dcutscene_fake_object_block_proc_change] [dcutscene_fake_object_block_proc_def_change] [dcutscene_fake_object_block_teleport] [] [] []
    - [] [] [] [dcutscene_fake_block_move_to_keyframe] [dcutscene_fake_block_duplicate] [dcutscene_fake_block_timespot_play] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_fake_object_block_remove] [] [] [] [dcutscene_exit]

#Fake schem modify GUI
dcutscene_inventory_fake_object_schem_modify:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [dcutscene_fake_object_schem_name_change] [dcutscene_fake_object_schem_loc_change] [dcutscene_fake_object_schem_duration_change] [dcutscene_fake_object_schem_noair_change] [dcutscene_fake_object_schem_waitable_change] [] []
    - [] [] [dcutscene_fake_object_schem_angle_change] [dcutscene_fake_schem_move_to] [dcutscene_fake_schem_duplicate] [dcutscene_fake_object_schem_teleport] [dcutscene_fake_schem_timespot_play] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_fake_object_schem_remove] [] [] [] [dcutscene_exit]

#Run Task GUI
dcutscene_inventory_keyframe_modify_run_task:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_run_task_change_modify] [dcutscene_run_task_def_modify] [dcutscene_run_task_wait_modify] [] [] []
    - [] [] [] [dcutscene_run_task_delay_modify] [dcutscene_run_task_move_to_keyframe] [dcutscene_run_task_duplicate] [] [] []
    - [] [] [] [dcutscene_run_task_timespot_play] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_run_task_remove_modify] [] [] [] [dcutscene_exit]

#Sound GUI
dcutscene_inventory_keyframe_modify_sound:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_sound_modify] [dcutscene_sound_volume_modify] [dcutscene_sound_pitch_modify] [] [] []
    - [] [] [] [dcutscene_sound_loc_modify] [dcutscene_sound_custom_modify] [dcutscene_sound_stop_modify] [] [] []
    - [] [] [] [dcutscene_sound_move_to_keyframe] [dcutscene_sound_duplicate] [dcutscene_sound_timespot_play] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_sound_remove_modify] [] [] [] [dcutscene_exit]

#Screeneffect GUI
dcutscene_inventory_keyframe_modify_screeneffect:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_screeneffect_time_modify] [dcutscene_screeneffect_color_modify] [dcutscene_screeneffect_move_to_keyframe] [] [] []
    - [] [] [] [dcutscene_screeneffect_duplicate] [dcutscene_screeneffect_timespot_play] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_screeneffect_remove_modify] [] [] [] [dcutscene_exit]

#Title GUI
dcutscene_inventory_keyframe_modify_title:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_title_title_modify] [dcutscene_title_subtitle_modify] [dcutscene_title_duration_modify] [] [] []
    - [] [] [] [dcutscene_title_move_to_keyframe] [dcutscene_title_duplicate] [dcutscene_title_timespot_play] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_title_remove_modify] [] [] [] [dcutscene_exit]

#Play command GUI
dcutscene_inventory_keyframe_modify_command:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_command_modify] [dcutscene_command_execute_as_modify] [dcutscene_command_silent_modify] [] [] []
    - [] [] [] [dcutscene_command_move_to_keyframe] [dcutscene_command_duplicate] [dcutscene_command_timespot_play] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_command_remove_modify] [] [] [] [dcutscene_exit]

#Send message GUI
dcutscene_inventory_keyframe_modify_message:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_message_modify] [dcutscene_message_move_to_keyframe] [dcutscene_message_duplicate] [] [] []
    - [] [] [] [dcutscene_message_timespot_play] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_message_remove_modify] [] [] [] [dcutscene_exit]

#Time GUI
dcutscene_inventory_keyframe_modify_time:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_time_modify] [dcutscene_time_duration_modify] [dcutscene_time_freeze_modify] [] [] []
    - [] [] [] [dcutscene_time_reset_modify] [dcutscene_time_move_to_keyframe] [dcutscene_time_duplicate] [] [] []
    - [] [] [] [dcutscene_time_timespot_play] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_time_remove_modify] [] [] [] [dcutscene_exit]

#Weather GUI
dcutscene_inventory_keyframe_modify_weather:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [dcutscene_weather_modify] [dcutscene_weather_duration_modify] [dcutscene_weather_move_to_keyframe] [] [] []
    - [] [] [] [dcutscene_weather_duplicate] [dcutscene_weather_timespot_play] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_weather_remove_modify] [] [] [] [dcutscene_exit]

#Play scene GUI
dcutscene_inventory_keyframe_modify_play_scene:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [dcutscene_play_scene_change] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [dcutscene_play_scene_remove] [] [] [] [dcutscene_exit]

#Stop point GUI
dcutscene_inventory_keyframe_modify_stop_point:
    type: inventory
    inventory: CHEST
    title: <&color[<script[dcutscenes_config].data_key[config].get[cutscene_title_color]>]><script[dcutscenes_config].data_key[config].get[cutscene_title]>
    size: 54
    gui: true
    slots:
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [dcutscene_stop_point_remove_modify] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [] [] [] [] [] [] [] [] []
    - [dcutscene_back_page] [] [] [] [] [] [] [] [dcutscene_exit]
####################################################