##########################################################################################
# This script file contains gui and regular events for the DCutscene GUI
# The cutscene_modify flag determines what will occur for the cutscene modification events
##########################################################################################

#======== Cutscene Events ==========
dcutscene_events:
    type: world
    debug: false
    events:
        on player quits:
        - if <player.has_flag[cutscene_modify]>:
          - flag <player> cutscene_modify:!
        - if <player.has_flag[dcutscene_location_editor]>:
          - flag <player> dcutscene_location_editor:!
        - if <player.has_flag[dcutscene_save_data]>:
          - define data <player.flag[dcutscene_save_data]>
          - define root <[data.root]||null>
          - if <[root]> != null:
            - define type <[data.type]>
            - choose <[type]>:
              - case player_model:
                - run pmodels_remove_model def:<[root]>
              - case model:
                - run dmodels_delete def:<[root]>
              - default:
                - remove <[root]>
          - flag <player> dcutscene_save_data:!
        ##Main cutscene gui ####
        after player right clicks block with:dcutscene_open_gui_item:
        - ~run dcutscene_scene_show def:back
        after player clicks dcutscene_keyframes_list in dcutscene_inventory_scene:
        - ~run dcutscene_keyframe_modify
        after player clicks dcutscene_play_cutscene_item in dcutscene_inventory_scene:
        - define cutscene <player.flag[cutscene_data.name]>
        - if <server.flag[dcutscenes.<[cutscene]>]||null> != null:
          - inventory close
          - run dcutscene_animation_begin def.scene:<[cutscene]> def.player:<player>
        - else:
          - debug error "Could not play scene <[cutscene]>."

        ##Settings #############
        #Settings page
        after player clicks dcutscene_settings in dcutscene_inventory_scene:
        - inventory open d:dcutscene_inventory_settings
        #Change cutscene name
        after player clicks dcutscene_change_scene_name in dcutscene_inventory_settings:
        - run dcutscene_settings_modify def:new_name_prep
        #Change cutscene description
        after player clicks dcutscene_change_description_item in dcutscene_inventory_settings:
        - run dcutscene_settings_modify def:change_desc_prep
        #Change cutscene bar bool
        after player clicks dcutscene_change_show_bars in dcutscene_inventory_settings:
        - run dcutscene_settings_modify def:change_bars|<context.slot>
        #Duplicate cutscene
        after player clicks dcutscene_duplicate_scene in dcutscene_inventory_settings:
        - run dcutscene_settings_modify def:duplicate_prep
        #Change cutscene GUI item
        after player clicks dcutscene_change_item in dcutscene_inventory_settings:
        - run dcutscene_settings_modify def:change_item_prep
        #Change hide players
        after player clicks dcutscene_hide_players in dcutscene_inventory_settings:
        - run dcutscene_settings_modify def:change_hide_players|<context.slot>
        #Change bound to camerea
        after player clicks dcutscene_bound_to_camera in dcutscene_inventory_settings:
        - run dcutscene_settings_modify def:change_camera_bound|<context.slot>
        #Remove cutscene from server flag
        after player clicks dcutscene_delete_cutscene in dcutscene_inventory_settings:
        - run dcutscene_settings_modify def:remove_scene_prep
        #Saves the cutscene
        after player clicks dcutscene_save_file_item in dcutscene_inventory_settings:
        - ratelimit <player> 2s
        - define cutscene <player.flag[cutscene_data]>
        - ~run dcutscene_save_file def:<[cutscene]>
        - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
        - define text "Cutscene <green><[cutscene.name].parse_color> <gray>has been saved to <green>Denizen/data/dcutscenes/scenes<gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        #Set the origin point
        after player clicks dcutscene_set_origin_point in dcutscene_inventory_settings:
        - run dcutscene_settings_modify def:set_origin_point_prep
        #Refresh location offsets
        after player clicks dcutscene_origin_point_refresh in dcutscene_inventory_settings:
        - ratelimit <player> 1s
        - define cutscene <player.flag[cutscene_data]>
        - ~run dcutscene_origin_set def.origin:<[cutscene.settings.origin]||false> def.scene:<[cutscene.name]> save:result
        - define loc_count <entry[result].created_queue.determination.first>
        - if <[loc_count].is_truthy>:
          - define text "<green><[loc_count]> <gray>location offsets have been refreshed."
          - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
          - narrate "<[msg_prefix]> <gray><[text]>"

        ##Misc #################
        #Exit page
        after player clicks dcutscene_exit in inventory:
        - inventory close

        ##Right click for location input in animator modifier
        after player right clicks block flagged:cutscene_modify using:hand:
        - ratelimit <player> 1t
        - choose <player.flag[cutscene_modify]>:
          #Sound location
          - case sound_location:
            - run dcutscene_animator_keyframe_edit def:sound|set_location|<context.location>
          #Set look location for camera
          - case create_present_cam_look_loc:
            - run dcutscene_cam_keyframe_edit def:edit|create_look_location|<context.location>
          #Fake block
          - case fake_block_location:
            - run dcutscene_animator_keyframe_edit def:fake_object|new_fake_block_loc|<context.location>
          - case set_fake_block_location:
            - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_loc|<context.location>
          #Fake schem
          - case new_fake_schem_loc:
            - run dcutscene_animator_keyframe_edit def:fake_object|new_schem_create|<context.location.center>
          - case change_fake_schem_loc:
            - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_loc|<context.location.center>
          #Particle
          - case new_particle_loc:
            - run dcutscene_animator_keyframe_edit def:particle|new_particle|<context.relative>
          - case change_particle_loc:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_loc|<context.relative>
          #Set Origin Point
          - case set_origin_point:
            - run dcutscene_settings_modify def:set_origin_point|<context.location.center>

        ##Tab completion ############
        after tab complete flagged:cutscene_modify:
        - define list <context.completions>
        - if <[list].contains[sound]>:
            - flag <player> cutscene_modify_tab:sound
        - else if <[list].contains[animate]>:
            - flag <player> cutscene_modify_tab:animate
        - else if <[list].contains[material]>:
            - flag <player> cutscene_modify_tab:material
        - else if <[list].contains[particle]>:
            - flag <player> cutscene_modify_tab:particle
        - else if <[list].contains[model]>:
            - flag <player> cutscene_modify_tab:model

        #Chat input for dcutscene gui animators
        ##Chat Input ################
        on player chats flagged:cutscene_modify:
        - define msg <context.message>
        - determine passively cancelled
        - if <[msg]> == cancel:
          - flag <player> cutscene_modify:!
          - flag <player> dcutscene_location_editor:!
          - flag <player> dcutscene_animator_change:!
          - if <player.gamemode> == spectator:
            - adjust <player> gamemode:creative
          - stop
        - choose <player.flag[cutscene_modify]>:
          ##Cutscene
          #New cutscene
          - case new_name:
            - run dcutscene_new_scene def.type:name def.scene:<[msg]>
          #Change name for cutscene
          - case cutscene_new_name:
            - run dcutscene_settings_modify def:change_name|<[msg]>
          #Modify description for present cutscene
          - case cutscene_new_desc:
            - run dcutscene_settings_modify def:change_desc|<[msg]>
          #Change cutscene GUI item
          - case cutscene_new_gui_item:
            - if <[msg]> == hand:
              - define item <player.item_in_hand>
            - else:
              - define item <[msg]>
            - run dcutscene_settings_modify def:change_item|<[item]>
          #Duplicate cutscene
          - case cutscene_duplicate:
            - run dcutscene_settings_modify def:duplicate|<[msg]>
          #Set origin point
          - case set_origin_point:
            - run dcutscene_settings_modify def:set_origin_point|<[msg]>

          ##Camera
          #Create new camera modifier
          - case create_cam:
            - if <[msg]> == confirm:
              - run dcutscene_cam_keyframe_edit def:create
          #Modify present camera in keyframe
          - case create_present_cam:
            - if <[msg]> == confirm:
              - run dcutscene_cam_keyframe_edit def:edit|create_new_location
          #Sets rotation multiplier for camera
          - case new_rotation_mul:
            - run dcutscene_cam_keyframe_edit def:edit|rotate_change|set_mul|<[msg]>
          #Input new look location
          - case create_present_cam_look_loc:
            - if <[msg]> == confirm:
              - run dcutscene_cam_keyframe_edit def:edit|create_look_location|<player.eye_location>
            - else if <[msg]> == false:
              - run dcutscene_cam_keyframe_edit def:edit|create_look_location|false
            - else:
              - run dcutscene_cam_keyframe_edit def:edit|create_look_location|<[msg]>
          #Set camera record to false
          - case camera_record_false:
            - if <[msg].equals[false]>:
              - run dcutscene_cam_keyframe_edit def:edit|record_camera_false|<[msg]>
          #Set duration for camera recorder
          - case camera_recorder_duration:
            - run dcutscene_cam_keyframe_edit def:edit|record_camera_begin|own|<[msg]>
          #Show camera path in cutscene
          - case camera_path:
            - if <[msg]> == stop:
              - flag <player> cutscene_modify:!

          ##Denizen models
          #Create new denizen model
          - case new_model_id:
            - run dcutscene_model_keyframe_edit def:denizen_model|create_id|<[msg]>
          #Set the newly created model into the dcutscene data including the location specified
          - case new_model_location:
            - if <[msg]> == confirm:
              - run dcutscene_model_keyframe_edit def:denizen_model|location_set_and_create_model|<player.flag[dcutscene_location_editor.location]>
          #Put new model keyframe point based on previous model
          - case new_model_keyframe_point:
            - if <[msg]> == confirm:
              - run dcutscene_model_keyframe_edit def:denizen_model|create_present|new_keyframe_set|<player.flag[dcutscene_location_editor.location]>
          #Change model ID
          - case change_model_id:
            - run dcutscene_model_keyframe_edit def:denizen_model|change_id|<[msg]>
          #Change model item
          - case change_model_item:
            - if <[msg]> == hand:
              - run dcutscene_model_keyframe_edit def:denizen_model|change_model_item|<player.item_in_hand>
            - else:
              - run dcutscene_model_keyframe_edit def:denizen_model|change_model_item|<[msg]>
          #Change model location
          - case set_new_model_location:
            - if <[msg]> == confirm:
              - run dcutscene_model_keyframe_edit def:denizen_model|change_location|<player.flag[dcutscene_location_editor.location]>
          #Stop model path from being shown
          - case model_path:
            - if <[msg]> == stop:
              - flag <player> cutscene_modify:!
          #Set rotation multiplier for model
          - case model_change_rotate_mul:
            - run dcutscene_model_keyframe_edit def:denizen_model|change_rotate_mul|<[msg]>

          ##Player model
          #Create new player model id
          - case new_player_model_id:
            - run dcutscene_model_keyframe_edit def:player_model|create|id_set|<[msg]>
          #Set new player model location
          - case new_player_model_location:
            - if <[msg]> == confirm:
              - run dcutscene_model_keyframe_edit def:player_model|create|location_set|<player.flag[dcutscene_location_editor.location]>
          #Put new player model keyframe point based on previous player model
          - case new_player_model_keyframe_point:
            - if <[msg]> == confirm:
              - run dcutscene_model_keyframe_edit def:player_model|create_present|new_keyframe_set|<player.flag[dcutscene_location_editor.location]>
          #Sets a new id for the player model
          - case set_player_model_id:
            - run dcutscene_model_keyframe_edit def:player_model|change_id|id_set|<[msg]>
          #Sets new location for already created player model
          - case set_new_player_model_location:
            - if <[msg]> == confirm:
              - run dcutscene_model_keyframe_edit def:player_model|location|set_location|<player.flag[dcutscene_location_editor.location]>
          #Set rotation multiplier
          - case player_model_change_rotate_mul:
            - run dcutscene_model_keyframe_edit def:player_model|change_rotate_mul|<[msg]>
          #Sets a new skin for the player model
          - case player_model_change_skin:
            - run dcutscene_model_keyframe_edit def:player_model|change_skin|set_new_skin|<[msg]>
          #Stop player model path from being shown
          - case player_model_path:
            - if <[msg]> == stop:
              - flag <player> cutscene_modify:!

          ##Screeneffect
          #Create new screeneffect modifier
          - case screeneffect:
            - run dcutscene_animator_keyframe_edit def:screeneffect|create|<[msg]>
          #Set new time
          - case screeneffect_time:
            - run dcutscene_animator_keyframe_edit def:screeneffect|set_time|<[msg]>
          #Set new color
          - case screeneffect_color:
            - run dcutscene_animator_keyframe_edit def:screeneffect|set_color|<[msg]>

          ##Run Task
          #Create new run task modifier
          - case run_task:
            - run dcutscene_animator_keyframe_edit def:run_task|create|<[msg]>
          #Change run task modifier script
          - case run_task_change:
            - run dcutscene_animator_keyframe_edit def:run_task|change_task|<[msg]>
          #Set definitions for run task
          - case run_task_def_set:
            - run dcutscene_animator_keyframe_edit def:run_task|set_task_definition|<[msg]>
          #Set delay for run task
          - case run_task_delay:
            - run dcutscene_animator_keyframe_edit def:run_task|change_delay|<[msg]>

          ##Fake block
          #New fake block location
          - case fake_block_location:
            - run dcutscene_animator_keyframe_edit def:fake_object|new_fake_block_loc|<[msg].parsed>
          #Set new fake block location for already created keyframe
          - case set_fake_block_location:
            - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_loc|<[msg].parsed>
          #Set fake block duration
          - case fake_block_duration:
            - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_duration|<[msg]>
          #Set procedure script for fake block
          - case fake_block_proc_script:
            - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_proc|<[msg]>
          #Set procedure definitions for fake block
          - case fake_block_proc_def:
            - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_proc_def|<[msg]>

          ##Fake Schematic
          #Input fake schem name
          - case new_fake_schem_name:
            - run dcutscene_animator_keyframe_edit def:fake_object|new_schem_loc|<[msg]>
          #Input fake schem location
          - case new_fake_schem_loc:
            - run dcutscene_animator_keyframe_edit def:fake_object|new_schem_create|<[msg].parsed.center||null>
          #Change fake schem name
          - case change_fake_schem_name:
            - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_name|<[msg]>
          #Change fake schem loc
          - case change_fake_schem_loc:
            - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_loc|<[msg].parsed.center||null>
          #Change fake schem duration
          - case change_fake_schem_duration:
            - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_duration|<[msg]>

          ##Particle
          #Input new particle location
          - case new_particle_loc:
            - run dcutscene_animator_keyframe_edit def:particle|new_particle|<[msg].parsed||null>
          #Input particle location
          - case change_particle_loc:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_loc|<[msg].parsed||null>
          #Input particle quantity
          - case change_particle_quantity:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_quantity|<[msg]>
          #Input particle visibility range
          - case change_particle_range:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_range|<[msg]>
          #Input particle repeat count
          - case change_particle_repeat_count:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_repeat_count|<[msg]>
          #Input particle repeat interval
          - case change_particle_repeat_interval:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_repeat_interval|<[msg]>
          #Input particle offset
          - case change_particle_offset:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_offset|<[msg].parsed>
          #Input particle procedure
          - case change_particle_proc_script:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_procedure_script|<[msg]>
          #Input particle definitions
          - case change_particle_proc_defs:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_procedure_defs|<[msg]>
          #Input particle special data
          - case change_particle_special_data:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_special_data|<[msg]>
          #Input particle velocity
          - case change_particle_velocity:
            - run dcutscene_animator_keyframe_edit def:particle|change_particle_velocity|<[msg].parsed>

          ##Title
          #Change title
          - case change_title:
            - run dcutscene_animator_keyframe_edit def:title|set_title|<[msg]>
          #Change subtitle
          - case change_subtitle:
            - run dcutscene_animator_keyframe_edit def:title|set_subtitle|<[msg]>
          #Change duration
          - case change_title_duration:
            - run dcutscene_animator_keyframe_edit def:title|set_duration|<[msg]>

          ##Command
          #New command
          - case new_command:
            - run dcutscene_animator_keyframe_edit def:command|new_command|<[msg]>
          #Change command
          - case change_command:
            - run dcutscene_animator_keyframe_edit def:command|change_command|<[msg]>

          ##Message
          #New message
          - case new_message:
            - run dcutscene_animator_keyframe_edit def:message|new_message|<[msg]>
          #Change message
          - case change_message:
            - run dcutscene_animator_keyframe_edit def:message|change_message|<[msg]>

          ##Sound
          #Input new volume for sound modifier
          - case sound_volume:
            - run dcutscene_animator_keyframe_edit def:sound|set_volume|<[msg]>
          #Input new pitch for sound modifier
          - case sound_pitch:
            - run dcutscene_animator_keyframe_edit def:sound|set_pitch|<[msg]>
          #Input new sound location
          - case sound_location:
            - if <[msg]> == confirm:
              - run dcutscene_animator_keyframe_edit def:sound|set_location|<player.location>
            - else if <[msg]> == false:
              - run dcutscene_animator_keyframe_edit def:sound|set_location|false
            - else:
              - run dcutscene_animator_keyframe_edit def:sound|set_location|<[msg]>

          ##Time
          #New time
          - case new_time:
            - run dcutscene_animator_keyframe_edit def:time|new_time|<[msg]>
          #Change time
          - case change_time:
            - run dcutscene_animator_keyframe_edit def:time|change_time|<[msg]>
          #Change time duration
          - case change_time_duration:
            - run dcutscene_animator_keyframe_edit def:time|change_time_duration|<[msg]>

          ##Weather
          #Change weather duration
          - case change_weather_duration:
            - run dcutscene_animator_keyframe_edit def:weather|change_weather_duration|<[msg]>

          ##Play Scene Animator
          #Set new play scene animator
          - case new_play_scene_animator:
            - run dcutscene_play_scene_keyframe_modify def:new|<[msg]>
          #Change scene to play
          - case change_scene_animator:
            - run dcutscene_play_scene_keyframe_modify def:change_scene|<[msg]>

        ##Main Keyframe GUI ####################
        # Create new scene
        after player clicks dcutscene_new_scene_item in dcutscene_inventory_main:
        - inventory close
        - run dcutscene_new_scene
        # Next page
        after player clicks dcutscene_next in dcutscene_inventory_main:
        - run dcutscene_scene_show def:next
        # Previous page
        after player clicks dcutscene_previous in dcutscene_inventory_main:
        - run dcutscene_scene_show def:previous
        # Open a cutscene in the GUI
        after player clicks item in dcutscene_inventory_main:
        - define i <context.item>
        - if <[i].has_flag[cutscene_data]>:
          - flag <player> cutscene_data:<server.flag[dcutscenes.<[i].flag[cutscene_data]>]>
          - inventory open d:dcutscene_inventory_scene
        # Open a main keyframe to display animators
        after player clicks item in dcutscene_inventory_keyframe:
        - define i <context.item>
        - if <[i].has_flag[keyframe_data]>:
          - flag <player> sub_keyframe_tick_page:0
          - inventory open d:dcutscene_inventory_sub_keyframe
          - ~run dcutscene_sub_keyframe_modify def:<[i].flag[keyframe_data]>

        ##Animators to modify click event
        after player clicks item in dcutscene_inventory_sub_keyframe:
        - define i <context.item>
        #=New keyframe modifier
        - if <[i].has_flag[keyframe_modify]>:
          - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_modify]>
          - inventory open d:dcutscene_inventory_keyframe_modify

        #=Modify present keyframe modifier
        - else if <[i].has_flag[keyframe_opt_modify]>:
          #Modify type
          - choose <[i].flag[keyframe_opt_modify.type]>:
            #Camera type
            - case camera:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify.tick]>
              - inventory open d:dcutscene_inventory_keyframe_modify_camera
            #Model type
            - case model:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_keyframe_modify_model
            #Player Model type
            - case player_model:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_keyframe_modify_player_model
            #Run Task Type
            - case run_task:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_keyframe_modify_run_task
            #Fake Block Type
            - case fake_block:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_fake_object_block_modify
            #Fake Schem Type
            - case fake_schem:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_fake_object_schem_modify
            #Particle Type
            - case particle:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_particle_modify
            #Sound type
            - case sound:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_keyframe_modify_sound
            #Screeneffect
            - case screeneffect:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_keyframe_modify_screeneffect
            #Title
            - case title:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify.tick]>
              - inventory open d:dcutscene_inventory_keyframe_modify_title
            #Command
            - case command:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_keyframe_modify_command
            #Message
            - case message:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_keyframe_modify_message
            #Time
            - case time:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify.tick]>
              - inventory open d:dcutscene_inventory_keyframe_modify_time
            #Weather
            - case weather:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify.tick]>
              - inventory open d:dcutscene_inventory_keyframe_modify_weather
            #Play scene
            - case play_scene:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify.tick]>
              - inventory open d:dcutscene_inventory_keyframe_modify_play_scene
            #Stop Point
            - case stop_point:
              - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_opt_modify]>
              - inventory open d:dcutscene_inventory_keyframe_modify_stop_point

        #=Used for moving or duplicating animators
        - else if <[i].has_flag[change_animator]>:
          - if <player.has_flag[dcutscene_animator_change]>:
            - flag <player> dcutscene_tick_modify:<[i].flag[keyframe_tick]>
            #TODO:
            #- Implement ability to move animators to another cutscene
            - define data <player.flag[dcutscene_animator_change]>
            - define cutscene_data <player.flag[cutscene_data]>
            - if <[data.scene]> != <[cutscene_data.name]>:
              - stop
            - define type <[data.type]>
            - choose <[data.animator]>:
              #Camera
              - case camera:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_cam_keyframe_edit def:edit|move_camera
                  - case duplicate:
                    - run dcutscene_cam_keyframe_edit def:edit|duplicate_camera
              #Denizen Model
              - case model:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_model_keyframe_edit def:denizen_model|move_to
                  - case duplicate:
                    - run dcutscene_model_keyframe_edit def:denizen_model|duplicate
              #Player Model
              - case player_model:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_model_keyframe_edit def:player_model|move_to
                  - case duplicate:
                    - run dcutscene_model_keyframe_edit def:player_model|duplicate
              #Run Task
              - case run_task:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:run_task|move_to
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:run_task|duplicate
              #Screeneffect
              - case screeneffect:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:screeneffect|move_to
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:screeneffect|duplicate
              #Sound
              - case sound:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:sound|move_to
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:sound|duplicate
              #Fake Block
              - case fake_block:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:fake_object|move_to_fake_block
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:fake_object|duplicate_fake_block
              #Fake Schem
              - case fake_schem:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:fake_object|move_to_fake_schem
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:fake_object|duplicate_fake_schem
              #Particle
              - case particle:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:particle|move_to
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:particle|duplicate
              #Title
              - case title:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:title|move_to
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:title|duplicate
              #Command
              - case command:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:command|move_to
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:command|duplicate
              #Message
              - case message:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:message|move_to
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:message|duplicate
              #Time
              - case time:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:time|move_to
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:time|duplicate
              #Weather
              - case weather:
                - choose <[type]>:
                  - case move:
                    - run dcutscene_animator_keyframe_edit def:weather|move_to
                  - case duplicate:
                    - run dcutscene_animator_keyframe_edit def:weather|duplicate

        #=List of models that were previously created
        #Previous models that were created and can be modified for further use
        after player clicks item in dcutscene_inventory_keyframe_model_list:
        - define i <context.item>
        - if <[i].has_flag[model_keyframe_modify]>:
          - define data <[i].flag[model_keyframe_modify]>
          - choose <[data.type]>:
            - case player_model:
              - run dcutscene_model_keyframe_edit def:player_model|create_present|new_keyframe_prepare|<[data]>
            - case model:
              - run dcutscene_model_keyframe_edit def:denizen_model|create_present|new_keyframe_prepare|<[data]>
        #Next page
        after player clicks dcutscene_next in dcutscene_inventory_keyframe_model_list:
        - run dcutscene_model_list_new def:<player.flag[dcutscene_tick_modify]>|<player.flag[dcutscene_save_data.scene_name]>|<player.flag[dcutscene_save_data.type]>|next
        #Previous page
        after player clicks dcutscene_previous in dcutscene_inventory_keyframe_model_list:
        - run dcutscene_model_list_new def:<player.flag[dcutscene_tick_modify]>|<player.flag[dcutscene_save_data.scene_name]>|<player.flag[dcutscene_save_data.type]>|previous

        #=Sub keyframe scroll up and down
        #Scroll up
        on player clicks dcutscene_scroll_up in dcutscene_inventory_sub_keyframe:
        - if !<player.has_flag[sub_keyframe_tick_page]>:
          - flag <player> sub_keyframe_tick_page:0
          - define tick_page 0
        - define tick_page <player.flag[sub_keyframe_tick_page].sub[4]>
        - if <[tick_page]> < 1:
          - define tick_page 0
        - flag <player> sub_keyframe_tick_page:<[tick_page]>
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Scroll down
        on player clicks dcutscene_scroll_down in dcutscene_inventory_sub_keyframe:
        - if !<player.has_flag[sub_keyframe_tick_page]>:
          - flag <player> sub_keyframe_tick_page:0
          - define tick_page 0
        - define tick_page <player.flag[sub_keyframe_tick_page].add[4]>
        - flag <player> sub_keyframe_tick_page:<[tick_page]>
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

        #========= Option GUI events ===========
        ##Camera #####
        #Add a new camera
        after player clicks dcutscene_add_cam in dcutscene_inventory_keyframe_modify:
        - run dcutscene_cam_keyframe_edit def:new
        #New location
        after player clicks dcutscene_camera_loc_modify in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|new_location
        #New look location
        after player clicks dcutscene_camera_look_modify in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|new_look_location
        #Teleport to camera
        after player clicks dcutscene_camera_teleport in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:teleport
        #Modify path interpolation
        after player clicks dcutscene_camera_interp_modify in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|interpolation_change|<context.item>|<context.slot>
        #Record camera
        after player clicks dcutscene_camera_record_player in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|record_camera_prep
        #Determine move
        after player clicks dcutscene_camera_move_modify in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|move_change|<context.item>|<context.slot>
        #Determine interpolate look
        after player clicks dcutscene_camera_interpolate_look in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|interpolate_look|<context.item>|<context.slot>
        #Determine Camera Look Rotation
        after player clicks dcutscene_camera_rotate_modify in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|rotate_change|new_mul
        #Determine camera look invert
        after player clicks dcutscene_camera_upside_down in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|invert_camera|<context.item>|<context.slot>
        #Show camera path
        after player clicks dcutscene_camera_path_show in dcutscene_inventory_keyframe_modify_camera:
        - inventory close
        - run dcutscene_path_show_interval def:camera
        #Move camera to new keyframe
        after player clicks dcutscene_camera_move_to_keyframe in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|move_camera_prep
        #Duplicate camera to new keyframe
        after player clicks dcutscene_camera_duplicate_to_keyframe in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|duplicate_camera_prep
        #Play cutscene from here
        after player clicks dcutscene_camera_timespot_play in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|play_from_here
        #Remove camera
        after player clicks dcutscene_camera_remove_modify in dcutscene_inventory_keyframe_modify_camera:
        - run dcutscene_cam_keyframe_edit def:edit|remove_camera

        ## All Models #######
        #New Model in model list gui
        after player clicks dcutscene_add_new_model in dcutscene_inventory_keyframe_model_list:
        - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
        - choose <player.flag[dcutscene_save_data.type]>:
          - case model:
            - flag <player> cutscene_modify:new_model_id expire:2m
            - define text "Chat the name of the model this will be used as an identifier."
            - narrate "<[msg_prefix]> <gray><[text]>"
            - inventory close
          - case player_model:
            - flag <player> cutscene_modify:new_player_model_id expire:2m
            - define text "Chat the name of the player model this will be used as an identifier."
            - narrate "<[msg_prefix]> <gray><[text]>"
            - inventory close
        #Confirms location for location tool
        after player clicks dcutscene_location_tool_confirm_location in dcutscene_inventory_location_tool:
        - choose <player.flag[cutscene_modify]>:
          #New location for brand new model
          - case new_player_model_location:
            - run dcutscene_model_keyframe_edit def:player_model|create|location_set|<player.flag[dcutscene_location_editor.location]>
          #New location for new model with a root model
          - case new_player_model_keyframe_point:
            - run dcutscene_model_keyframe_edit def:player_model|create_present|new_keyframe_set|<player.flag[dcutscene_location_editor.location]>
          #Setting a new location for the already created model
          - case set_new_player_model_location:
            - run dcutscene_model_keyframe_edit def:player_model|location|set_location|<player.flag[dcutscene_location_editor.location]>

        ## Denizen Models ####
        #New model
        after player clicks dcutscene_add_model in dcutscene_inventory_keyframe_modify:
        - run dcutscene_model_keyframe_edit def:denizen_model|new
        #Change ID
        after player clicks dcutscene_model_change_id in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|change_id_prep
        #Move to a new keyframe
        after player clicks dcutscene_model_move_to_keyframe in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|move_to_prep
        #Duplicate
        after player clicks dcutscene_model_duplicate in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|duplicate_prep
        #Change model GUI item
        after player clicks dcutscene_model_change_item in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|change_model_item_prep
        #Change model location
        after player clicks dcutscene_model_change_location in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|change_location_prep
        #Ray Trace GUI
        after player clicks dcutscene_model_ray_trace_change in dcutscene_inventory_keyframe_modify_model:
        - inventory open d:dcutscene_inventory_keyframe_ray_trace_model
        #Change ray trace direction
        after player clicks dcutscene_model_ray_trace_determine in dcutscene_inventory_keyframe_ray_trace_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|ray_trace|ray_trace_direction|<context.slot>
        #Change ray trace liquid
        after player clicks dcutscene_model_ray_trace_liquid in dcutscene_inventory_keyframe_ray_trace_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|ray_trace|ray_trace_liquid|<context.slot>
        #Change ray trace passable
        after player clicks dcutscene_model_ray_trace_passable in dcutscene_inventory_keyframe_ray_trace_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|ray_trace|ray_trace_passable|<context.slot>
        #Change model
        after player clicks dcutscene_model_change_model in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|change_model_prep
        #Change move
        after player clicks dcutscene_model_change_move in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|set_move|<context.slot>
        #Change animation
        after player clicks dcutscene_model_change_animation in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|new_animation_prepare
        #Change path interpolation method
        after player clicks dcutscene_model_interp_method in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|change_path_interp|<context.slot>
        #Path shower
        after player clicks dcutscene_model_show_path in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_path_show_interval def:model|<player.flag[dcutscene_tick_modify.tick]>|<player.flag[dcutscene_tick_modify.uuid]>
        - inventory close
        #Change path rotation interpolation
        after player clicks dcutscene_model_interp_rotate_change in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|change_rotate_interp|<context.slot>
        #Change path rotation multiplier
        after player clicks dcutscene_model_interp_rotate_mul in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|change_rotate_mul_prep
        #Teleport to model location
        after player clicks dcutscene_model_teleport_loc in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|teleport_to
        #Play from this tick
        after player clicks dcutscene_model_timespot_play in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|play_from_here
        #Remove from tick
        after player clicks dcutscene_remove_model_tick in dcutscene_inventory_keyframe_modify_model:
        - run dcutscene_model_keyframe_edit def:denizen_model|remove_tick
        #Remove from cutscene
        after player clicks dcutscene_remove_model in dcutscene_inventory_keyframe_modify_model:
        - inventory close
        - clickable dcutscene_model_keyframe_edit def:denizen_model|remove_all usages:1 save:remove_model
        - define prefix <element[DCutscenes].color_gradient[from=blue;to=aqua].bold>
        - define text "Are you sure you want to remove this model? <green><bold><element[Yes].on_hover[<[prefix]> <gray>This will permanently remove this model from this scene.].type[SHOW_TEXT].on_click[<entry[remove_model].command>]>"
        - narrate "<[prefix]> <gray><[text]>"

        ## Player Model ####
        #New player model
        after player clicks dcutscene_add_player_model in dcutscene_inventory_keyframe_modify:
        - run dcutscene_model_keyframe_edit def:player_model|new
        #Set animation
        after player clicks dcutscene_player_model_change_animation in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|animate|new_animation_prepare
        #Change move
        after player clicks dcutscene_player_model_change_move in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|set_move|<context.slot>
        #Change location
        after player clicks dcutscene_player_model_change_location in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|location|new_location_prepare
        #Move to a new keyframe
        after player clicks dcutscene_player_model_move_to_keyframe in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|move_to_prep
        #Duplicate
        after player clicks dcutscene_player_model_duplicate in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|duplicate_prep
        #Preparation for new player model id
        after player clicks dcutscene_player_model_change_id in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|change_id|new_id_prepare
        #Change path interpolation method
        after player clicks dcutscene_player_model_interp_method in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|change_path_interp|<context.slot>
        #Ray Trace GUI
        after player clicks dcutscene_player_model_ray_trace_change in dcutscene_inventory_keyframe_modify_player_model:
        - inventory open d:dcutscene_inventory_keyframe_ray_trace_player_model
        #Change ray trace direction
        after player clicks dcutscene_player_model_ray_trace_determine in dcutscene_inventory_keyframe_ray_trace_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|ray_trace|ray_trace_direction|<context.slot>
        #Change ray trace liquid
        after player clicks dcutscene_player_model_ray_trace_liquid in dcutscene_inventory_keyframe_ray_trace_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|ray_trace|ray_trace_liquid|<context.slot>
        #Change ray trace passable
        after player clicks dcutscene_player_model_ray_trace_passable in dcutscene_inventory_keyframe_ray_trace_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|ray_trace|ray_trace_passable|<context.slot>
        #Change the player model skin
        after player clicks dcutscene_player_model_change_skin in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|change_skin|new_skin_prepare
        #Change path rotation interpolation
        after player clicks dcutscene_player_model_interp_rotate_change in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|change_rotate_interp|<context.slot>
        #Change path rotation multiplier
        after player clicks dcutscene_player_model_interp_rotate_mul in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|change_rotate_mul_prep
        #Teleport to player model location
        after player clicks dcutscene_player_model_teleport_loc in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|teleport_to
        #Path shower
        after player clicks dcutscene_player_model_show_path in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_path_show_interval def:player_model|<player.flag[dcutscene_tick_modify.tick]>|<player.flag[dcutscene_tick_modify.uuid]>
        - inventory close
        #Play from this tick
        after player clicks dcutscene_player_model_timespot_play in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|play_from_here
        #Remove from tick
        after player clicks dcutscene_remove_player_model_tick in dcutscene_inventory_keyframe_modify_player_model:
        - run dcutscene_model_keyframe_edit def:player_model|remove_tick
        #Remove from cutscene
        after player clicks dcutscene_remove_player_model in dcutscene_inventory_keyframe_modify_player_model:
        - inventory close
        - clickable dcutscene_model_keyframe_edit def:player_model|remove_all usages:1 save:remove_model
        - define prefix <element[DCutscenes].color_gradient[from=blue;to=aqua].bold>
        - define text "Are you sure you want to remove this player model? <green><bold><element[Yes].on_hover[<[prefix]> <gray>This will permanently remove this player model from this scene.].type[SHOW_TEXT].on_click[<entry[remove_model].command>]>"
        - narrate "<[prefix]> <gray><[text]>"

        ## Run Task ######
        #Add new run task
        after player clicks dcutscene_add_run_task in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:run_task|new
        #Change the run task script
        after player clicks dcutscene_run_task_change_modify in dcutscene_inventory_keyframe_modify_run_task:
        - run dcutscene_animator_keyframe_edit def:run_task|change_task_prepare
        #Set definitions for run task
        after player clicks dcutscene_run_task_def_modify in dcutscene_inventory_keyframe_modify_run_task:
        - run dcutscene_animator_keyframe_edit def:run_task|task_definition
        #Determine if run task is waitable
        after player clicks dcutscene_run_task_wait_modify in dcutscene_inventory_keyframe_modify_run_task:
        - run dcutscene_animator_keyframe_edit def:run_task|change_waitable|<context.item>|<context.slot>
        #Determine if the run task is delayed
        after player clicks dcutscene_run_task_delay_modify in dcutscene_inventory_keyframe_modify_run_task:
        - run dcutscene_animator_keyframe_edit def:run_task|delay_prepare
        #Move to new tick
        after player clicks dcutscene_run_task_move_to_keyframe in dcutscene_inventory_keyframe_modify_run_task:
        - run dcutscene_animator_keyframe_edit def:run_task|move_to_prep
        #Duplicate run task
        after player clicks dcutscene_run_task_duplicate in dcutscene_inventory_keyframe_modify_run_task:
        - run dcutscene_animator_keyframe_edit def:run_task|duplicate_prep
        #Play from this tick
        after player clicks dcutscene_run_task_timespot_play in dcutscene_inventory_keyframe_modify_run_task:
        - run dcutscene_animator_keyframe_edit def:run_task|play_from_here
        #Remove the run task
        after player clicks dcutscene_run_task_remove_modify in dcutscene_inventory_keyframe_modify_run_task:
        - run dcutscene_animator_keyframe_edit def:run_task|remove

        ## Sound #####
        #Add new sound
        after player clicks dcutscene_add_sound in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:sound|new
        #New volume
        after player clicks dcutscene_sound_volume_modify in dcutscene_inventory_keyframe_modify_sound:
        - run dcutscene_animator_keyframe_edit def:sound|new_volume
        #New pitch
        after player clicks dcutscene_sound_pitch_modify in dcutscene_inventory_keyframe_modify_sound:
        - run dcutscene_animator_keyframe_edit def:sound|new_pitch
        #Determine custom
        after player clicks dcutscene_sound_custom_modify in dcutscene_inventory_keyframe_modify_sound:
        - run dcutscene_animator_keyframe_edit def:sound|set_custom|<context.item>|<context.slot>
        #New location
        after player clicks dcutscene_sound_loc_modify in dcutscene_inventory_keyframe_modify_sound:
        - run dcutscene_animator_keyframe_edit def:sound|new_location
        #Move to new tick
        after player clicks dcutscene_sound_move_to_keyframe in dcutscene_inventory_keyframe_modify_sound:
        - run dcutscene_animator_keyframe_edit def:sound|move_to_prep
        #Duplicate
        after player clicks dcutscene_sound_duplicate in dcutscene_inventory_keyframe_modify_sound:
        - run dcutscene_animator_keyframe_edit def:sound|duplicate_prep
        #Play from this tick
        after player clicks dcutscene_sound_timespot_play in dcutscene_inventory_keyframe_modify_sound:
        - run dcutscene_animator_keyframe_edit def:sound|play_from_here
        #Remove sound
        after player clicks dcutscene_sound_remove_modify in dcutscene_inventory_keyframe_modify_sound:
        - run dcutscene_animator_keyframe_edit def:sound|remove_sound

        ## Cinematic Screeneffect #####
        #Add new cinematic screeneffect
        after player clicks dcutscene_add_screeneffect in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:screeneffect|new
        #Set cinematic screeneffect time for present modifier
        after player clicks dcutscene_screeneffect_time_modify in dcutscene_inventory_keyframe_modify_screeneffect:
        - run dcutscene_animator_keyframe_edit def:screeneffect|new_time
        #Set cinematic screeneffect color for present modifier
        after player clicks dcutscene_screeneffect_color_modify in dcutscene_inventory_keyframe_modify_screeneffect:
        - run dcutscene_animator_keyframe_edit def:screeneffect|new_color
        #Move to new keyframe
        after player clicks dcutscene_screeneffect_move_to_keyframe in dcutscene_inventory_keyframe_modify_screeneffect:
        - run dcutscene_animator_keyframe_edit def:screeneffect|move_to_prep
        #Move to another keyframe
        after player clicks dcutscene_screeneffect_duplicate in dcutscene_inventory_keyframe_modify_screeneffect:
        - run dcutscene_animator_keyframe_edit def:screeneffect|duplicate_prep
        #Play from here
        after player clicks dcutscene_screeneffect_timespot_play in dcutscene_inventory_keyframe_modify_screeneffect:
        - run dcutscene_animator_keyframe_edit def:screeneffect|play_from_here
        #Remove cinematic screeneffect
        after player clicks dcutscene_screeneffect_remove_modify in dcutscene_inventory_keyframe_modify_screeneffect:
        - run dcutscene_animator_keyframe_edit def:screeneffect|remove

        #==== Fake Object ====
        ##Fake Block ##
        #Add new fake block
        after player clicks dcutscene_add_fake_block in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|new_fake_block_material
        #Change location
        after player clicks dcutscene_fake_object_block_loc_change in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_prepare
        #Change material
        after player clicks dcutscene_fake_object_block_material_change in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_material_prep
        #Change duration
        after player clicks dcutscene_fake_object_block_duration_change in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_duration_prepare
        #Set procedure script
        after player clicks dcutscene_fake_object_block_proc_change in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_proc_prepare
        #Set procedure definitions
        after player clicks dcutscene_fake_object_block_proc_def_change in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_proc_def_prepare
        #Teleport to fake block
        after player clicks dcutscene_fake_object_block_teleport in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|teleport_to_fake_block
        #Move fake block animator
        after player clicks dcutscene_fake_block_move_to_keyframe in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|move_to_fake_block_prep
        #Duplicate fake block animator
        after player clicks dcutscene_fake_block_duplicate in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|duplicate_fake_block_prep
        #Play from here
        after player clicks dcutscene_fake_block_timespot_play in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|fake_block_play_from_here
        #Remove the fake block
        after player clicks dcutscene_fake_object_block_remove in dcutscene_inventory_fake_object_block_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|remove_fake_block

        ##Fake Schem ###
        #Add new fake schem
        after player clicks dcutscene_add_fake_schem in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|new_schem_name
        #Change fake schem name
        after player clicks dcutscene_fake_object_schem_name_change in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_name_prep
        #Change fake schem loc
        after player clicks dcutscene_fake_object_schem_loc_change in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_loc_prep
        #Change fake schem duration
        after player clicks dcutscene_fake_object_schem_duration_change in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_duration_prep
        #Change fake schem noair
        after player clicks dcutscene_fake_object_schem_noair_change in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_noair|<context.slot>
        #Change fake schem waitable
        after player clicks dcutscene_fake_object_schem_waitable_change in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_waitable|<context.slot>
        #Change fake schem direction
        after player clicks dcutscene_fake_object_schem_angle_change in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|change_schem_direction|<context.slot>
        #Teleport to fake schem location
        after player clicks dcutscene_fake_object_schem_teleport in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|teleport_to_fake_schem
        #Move to new keyframe
        after player clicks dcutscene_fake_schem_move_to in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|move_to_fake_schem_prep
        #Duplicate
        after player clicks dcutscene_fake_schem_duplicate in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|duplicate_fake_schem_prep
        #Play from here
        after player clicks dcutscene_fake_schem_timespot_play in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|fake_schem_play_from_here
        #Remove fake schem
        after player clicks dcutscene_fake_object_schem_remove in dcutscene_inventory_fake_object_schem_modify:
        - run dcutscene_animator_keyframe_edit def:fake_object|remove_fake_schem

        ## Particle ######
        #Add new particle
        after player clicks dcutscene_add_particle in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:particle|new_particle_prep
        #Change particle
        after player clicks dcutscene_particle_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_prep
        #Change location
        after player clicks dcutscene_particle_loc_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_loc_prep
        #Change quantity
        after player clicks dcutscene_particle_quantity_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_quantity_prep
        #Change visible range
        after player clicks dcutscene_particle_range_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_range_prep
        #Change repeat count
        after player clicks dcutscene_particle_repeat_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_repeat_count_prep
        #Change repeat interval
        after player clicks dcutscene_particle_repeat_interval_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_repeat_interval_prep
        #Change particle offset
        after player clicks dcutscene_particle_offset_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_offset_prep
        #Change particle procedure script
        after player clicks dcutscene_particle_procedure_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_procedure_script_prep
        #Change particle procedure definitions
        after player clicks dcutscene_particle_procedure_defs_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_procedure_defs_prep
        #Change particle special data
        after player clicks dcutscene_particle_special_data_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_special_data_prep
        #Change particle velocity
        after player clicks dcutscene_particle_velocity_modify in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|change_particle_velocity_prep
        #Teleport to particle
        after player clicks dcutscene_particle_teleport_to in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|teleport_to_particle
        #Move to new keyframe
        after player clicks dcutscene_particle_move_to_keyframe in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|move_to_prep
        #Duplicate
        after player clicks dcutscene_particle_duplicate in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|duplicate_prep
        #Play from here
        after player clicks dcutscene_particle_timespot_play in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|play_from_here
        #Remove particle
        after player clicks dcutscene_particle_remove in dcutscene_inventory_particle_modify:
        - run dcutscene_animator_keyframe_edit def:particle|remove_particle

        ## Title ######
        #Add new title
        after player clicks dcutscene_send_title in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:title|new_title
        #Change title
        after player clicks dcutscene_title_title_modify in dcutscene_inventory_keyframe_modify_title:
        - run dcutscene_animator_keyframe_edit def:title|set_title_prep
        #Change subtitle
        after player clicks dcutscene_title_subtitle_modify in dcutscene_inventory_keyframe_modify_title:
        - run dcutscene_animator_keyframe_edit def:title|set_subtitle_prep
        #Change duration
        after player clicks dcutscene_title_duration_modify in dcutscene_inventory_keyframe_modify_title:
        - run dcutscene_animator_keyframe_edit def:title|set_duration_prep
        #Move to a new keyframe
        after player clicks dcutscene_title_move_to_keyframe in dcutscene_inventory_keyframe_modify_title:
        - run dcutscene_animator_keyframe_edit def:title|move_to_prep
        #Duplicate
        after player clicks dcutscene_title_duplicate in dcutscene_inventory_keyframe_modify_title:
        - run dcutscene_animator_keyframe_edit def:title|duplicate_prep
        #Play from here
        after player clicks dcutscene_title_timespot_play in dcutscene_inventory_keyframe_modify_title:
        - run dcutscene_animator_keyframe_edit def:title|play_from_here
        #Remove title
        after player clicks dcutscene_title_remove_modify in dcutscene_inventory_keyframe_modify_title:
        - run dcutscene_animator_keyframe_edit def:title|remove_title

        ## Command ####
        #Add new command
        after player clicks dcutscene_play_command in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:command|new_command_prep
        #Change command
        after player clicks dcutscene_command_modify in dcutscene_inventory_keyframe_modify_command:
        - run dcutscene_animator_keyframe_edit def:command|change_command_prep
        #Change execute_as
        after player clicks dcutscene_command_execute_as_modify in dcutscene_inventory_keyframe_modify_command:
        - run dcutscene_animator_keyframe_edit def:command|change_execute_as|<context.slot>
        #Change silent command
        after player clicks dcutscene_command_silent_modify in dcutscene_inventory_keyframe_modify_command:
        - run dcutscene_animator_keyframe_edit def:command|change_silent|<context.slot>
        #Move to new keyframe
        after player clicks dcutscene_command_move_to_keyframe in dcutscene_inventory_keyframe_modify_command:
        - run dcutscene_animator_keyframe_edit def:command|move_to_prep
        #Duplicate
        after player clicks dcutscene_command_duplicate in dcutscene_inventory_keyframe_modify_command:
        - run dcutscene_animator_keyframe_edit def:command|duplicate_prep
        #Play from here
        after player clicks dcutscene_command_timespot_play in dcutscene_inventory_keyframe_modify_command:
        - run dcutscene_animator_keyframe_edit def:command|play_from_here
        #Remove command
        after player clicks dcutscene_command_remove_modify in dcutscene_inventory_keyframe_modify_command:
        - run dcutscene_animator_keyframe_edit def:command|remove_command

        ## Message ####
        #New message
        after player clicks dcutscene_add_msg in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:message|new_message_prep
        #Change message
        after player clicks dcutscene_message_modify in dcutscene_inventory_keyframe_modify_message:
        - run dcutscene_animator_keyframe_edit def:message|change_message_prep
        #Move to a keyframe
        after player clicks dcutscene_message_move_to_keyframe in dcutscene_inventory_keyframe_modify_message:
        - run dcutscene_animator_keyframe_edit def:message|move_to_prep
        #Duplicate
        after player clicks dcutscene_message_duplicate in dcutscene_inventory_keyframe_modify_message:
        - run dcutscene_animator_keyframe_edit def:message|duplicate_prep
        #Play from here
        after player clicks dcutscene_message_timespot_play in dcutscene_inventory_keyframe_modify_message:
        - run dcutscene_animator_keyframe_edit def:message|play_from_here
        #Remove message
        after player clicks dcutscene_message_remove_modify in dcutscene_inventory_keyframe_modify_message:
        - run dcutscene_animator_keyframe_edit def:message|remove_message

        ## Time ####
        #Add time
        after player clicks dcutscene_send_time in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:time|new_time_prep
        #Set time
        after player clicks dcutscene_time_modify in dcutscene_inventory_keyframe_modify_time:
        - run dcutscene_animator_keyframe_edit def:time|change_time_prep
        #Set duration
        after player clicks dcutscene_time_duration_modify in dcutscene_inventory_keyframe_modify_time:
        - run dcutscene_animator_keyframe_edit def:time|change_time_duration_prep
        #Change time freeze
        after player clicks dcutscene_time_freeze_modify in dcutscene_inventory_keyframe_modify_time:
        - run dcutscene_animator_keyframe_edit def:time|change_time_freeze|<context.slot>
        #Change time reset
        after player clicks dcutscene_time_reset_modify in dcutscene_inventory_keyframe_modify_time:
        - run dcutscene_animator_keyframe_edit def:time|change_time_reset|<context.slot>
        #Move to a keyframe
        after player clicks dcutscene_time_move_to_keyframe in dcutscene_inventory_keyframe_modify_time:
        - run dcutscene_animator_keyframe_edit def:time|move_to_prep
        #Duplicate
        after player clicks dcutscene_time_duplicate in dcutscene_inventory_keyframe_modify_time:
        - run dcutscene_animator_keyframe_edit def:time|duplicate_prep
        #Play from here
        after player clicks dcutscene_time_timespot_play in dcutscene_inventory_keyframe_modify_time:
        - run dcutscene_animator_keyframe_edit def:time|play_from_here
        #Remove time
        after player clicks dcutscene_time_remove_modify in dcutscene_inventory_keyframe_modify_time:
        - run dcutscene_animator_keyframe_edit def:time|remove_time

        ## Weather ####
        #Add weather
        after player clicks dcutscene_set_weather in dcutscene_inventory_keyframe_modify:
        - run dcutscene_animator_keyframe_edit def:weather|new_weather
        #Set weather
        after player clicks dcutscene_weather_modify in dcutscene_inventory_keyframe_modify_weather:
        - run dcutscene_animator_keyframe_edit def:weather|change_weather|<context.slot>
        #Set weather duration
        after player clicks dcutscene_weather_duration_modify in dcutscene_inventory_keyframe_modify_weather:
        - run dcutscene_animator_keyframe_edit def:weather|change_weather_duration_prep
        #Move to a keyframe
        after player clicks dcutscene_weather_move_to_keyframe in dcutscene_inventory_keyframe_modify_weather:
        - run dcutscene_animator_keyframe_edit def:weather|move_to_prep
        #Duplicate
        after player clicks dcutscene_weather_duplicate in dcutscene_inventory_keyframe_modify_weather:
        - run dcutscene_animator_keyframe_edit def:weather|duplicate_prep
        #Play from here
        after player clicks dcutscene_weather_timespot_play in dcutscene_inventory_keyframe_modify_weather:
        - run dcutscene_animator_keyframe_edit def:weather|play_from_here
        #Remove weather
        after player clicks dcutscene_weather_remove_modify in dcutscene_inventory_keyframe_modify_weather:
        - run dcutscene_animator_keyframe_edit def:weather|remove_weather

        ## Play Scene Animator #####
        #Add new play scene animator
        after player clicks dcutscene_play_scene in dcutscene_inventory_keyframe_modify:
        - run dcutscene_play_scene_keyframe_modify def:new_prep
        #Change scene to play
        after player clicks dcutscene_play_scene_change in dcutscene_inventory_keyframe_modify_play_scene:
        - run dcutscene_play_scene_keyframe_modify def:change_scene_prep
        #Remove play scene animator
        after player clicks dcutscene_play_scene_remove in dcutscene_inventory_keyframe_modify_play_scene:
        - run dcutscene_play_scene_keyframe_modify def:remove

        ## Stop Point #####
        #Add new stop point
        after player clicks dcutscene_stop_scene in dcutscene_inventory_keyframe_modify:
        - run dcutscene_stop_scene_keyframe def:new
        #Remove stop point
        after player clicks dcutscene_stop_point_remove_modify in dcutscene_inventory_keyframe_modify_stop_point:
        - run dcutscene_stop_scene_keyframe def:remove

        ##Next and previous buttons in main keyframe list ###
        after player clicks dcutscene_next in dcutscene_inventory_keyframe:
        - ~run dcutscene_keyframe_modify def:next
        after player clicks dcutscene_previous in dcutscene_inventory_keyframe:
        - ~run dcutscene_keyframe_modify def:previous

        ###############################
        ##Back page functions ##########
        #Sub-keyframe
        after player clicks dcutscene_back_page in dcutscene_inventory_sub_keyframe:
        - ~run dcutscene_keyframe_modify def:back
        #Main-keyframe
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe:
        - inventory open d:dcutscene_inventory_scene
        #Scene GUI
        after player clicks dcutscene_back_page in dcutscene_inventory_scene:
        - run dcutscene_scene_show def:back
        #Settings page
        after player clicks dcutscene_back_page in dcutscene_inventory_settings:
        - inventory open d:dcutscene_inventory_scene
        #Camera
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_camera:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Model
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_model:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Player Model
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_player_model:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Run Task
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_run_task:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Sound
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_sound:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Screeneffect
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_screeneffect:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #GUI containing animators to add
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Model list gui
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_model_list:
        - inventory open d:dcutscene_inventory_keyframe_modify
        #Ray trace model GUI
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_ray_trace_model:
        - inventory open d:dcutscene_inventory_keyframe_modify_model
        #Ray trace player model GUI
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_ray_trace_player_model:
        - inventory open d:dcutscene_inventory_keyframe_modify_player_model
        #Fake block
        after player clicks dcutscene_back_page in dcutscene_inventory_fake_object_block_modify:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Play Scene
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_play_scene:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Stop point
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_stop_point:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Fake schematic
        after player clicks dcutscene_back_page in dcutscene_inventory_fake_object_schem_modify:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Particle
        after player clicks dcutscene_back_page in dcutscene_inventory_particle_modify:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Title
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_title:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Command
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_command:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Message
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_message:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Time
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_time:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        #Weather
        after player clicks dcutscene_back_page in dcutscene_inventory_keyframe_modify_weather:
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>