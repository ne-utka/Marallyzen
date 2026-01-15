#################################################
# This script file modifies the animators in the cutscene.
#################################################

# Data Utilized:
#- <server.flag[dcutscenes]> - "All the cutscenes the server has with specific use as <server.flag[dcutscenes.my_scene]>"
#- <player.flag[cutscene_data]> - "Returns the cutscene the creator is modifying"
#- <player.flag[cutscene_data.keyframes]> - "Returns the animators within the cutscene"
#- <player.flag[cutscene_data.name]> - "Returns the name of the cutscene"
#- <player.flag[dcutscene_tick_modify]> - "Returns the tick the creator is modifying without a uuid this is generally used when creating a new animator"
#- <player.flag[dcutscene_tick_modify.tick]> - "Returns the tick if animator uuid is specified as well"
#- <player.flag[dcutscene_tick_modify.uuid]> - "Returns the uuid of the animator the creator is modifying this is generally used for list capable animators"
#- <player.flag[dcutscene_save_data]> - "Used for multi operation keyframe modifiers in keeping data"
#- <player.flag[dcutscene_animator_change]> - "Used when moving or duplicating animators to a new tick"
#- <player.flag[cutscene_modify]> - "Used for event handlers or the dcutscene command with a specified key such as change_sound to modify an animator"
#- <player.flag[dcutscene_location_editor]> "Returns the data of the creator's location tool this is generally used for the model animator"

#=To get a better understanding how the data structure is turn off save file compression in the config, save the cutscene and read the json file.

#=The debugger mode when on is useful in viewing the modified data in a file without changing the server flag dcutscenes and accidently creating corrupt data.

#============= Keyframe Modifiers ===============

#======== Play Another Scene Modifier ========
dcutscene_play_scene_keyframe_modify:
    type: task
    debug: false
    definitions: option|arg
    script:
    - define data <player.flag[cutscene_data]>
    - define tick <player.flag[dcutscene_tick_modify]>
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - choose <[option]>:
      #-New play scene animator prep
      - case new_prep:
        - define text "Chat the cutscene the animator will play."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - flag <player> cutscene_modify:new_play_scene_animator expire:3m
        - inventory close

      #-New play cutscene animator
      - case new:
        - define scene_check <[data.keyframes.play_scene]||null>
        - if <[scene_check]> != null:
          - define text "There is already a play cutscene animator at tick <green><[scene_check.tick]>t<gray>."
          - narrate "<[msg_prefix]> <gray><[text]>"
        - else:
          - if <server.flag[dcutscenes.<[arg]>]||null> == null:
            - define text "The cutscene <green><[arg]><gray> does not exist."
            - narrate "<[msg_prefix]> <gray><[text]>"
            - stop
          - definemap play_scene_data tick:<[tick]> cutscene:<[arg]>
          - define data.keyframes.play_scene:<[play_scene_data]>
          - flag server dcutscenes.<[data.name]>:<[data]>
          - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
          - inventory open d:dcutscene_inventory_sub_keyframe
          - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
          - define text "The scene <green><[arg]> <gray>will now play at tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
          - narrate "<[msg_prefix]> <gray><[text]>"
          - ~run dcutscene_sort_data def.cutscene:<[data.name]>

      #-Change scene prep
      - case change_scene_prep:
        - define text "Chat the cutscene the animator will play."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - flag <player> cutscene_modify:change_scene_animator expire:3m
        - inventory close

      #-Change scene
      - case change_scene:
        - if <server.flag[dcutscenes.<[arg]>]||null> == null:
          - define text "The cutscene <green><[arg]><gray> does not exist."
          - narrate "<[msg_prefix]> <gray><[text]>"
          - stop
        - define play_scene <[data.keyframes.play_scene]>
        - define play_scene.cutscene <[arg]>
        - define data.keyframes.play_scene:<[play_scene]>
        - flag server dcutscenes.<[data.name]>:<[data]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
        - inventory open d:dcutscene_inventory_keyframe_modify_play_scene
        - define text "The scene <green><[arg]> <gray>will now play at tick <green><[play_scene.tick]>t <gray>for scene <green><[data.name]><gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"

      #-Remove play cutscene animator
      - case remove:
        - define data.keyframes <[data.keyframes].exclude[play_scene]>
        - flag server dcutscenes.<[data.name]>:<[data]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        - define text "Cutscene play scene animator has been removed from tick <green><[tick]>t<gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - ~run dcutscene_sort_data def.cutscene:<[data.name]>

#======== Stop Cutscene Modifier ========
dcutscene_stop_scene_keyframe:
    type: task
    debug: false
    definitions: option|arg
    script:
    - define data <player.flag[cutscene_data]>
    - define tick <player.flag[dcutscene_tick_modify]>
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - choose <[option]>:
      #-New stop scene keyframe
      - case new:
        - define stop_check <[data.keyframes.stop]||null>
        #There can only be 1 stop point
        - if <[stop_check]> != null:
          - define text "There is already a cutscene stop point at tick <green><[stop_check.tick]>t<gray>."
          - narrate "<[msg_prefix]> <gray><[text]>"
        - else:
          - define data.keyframes.stop.tick <[tick]>
          - flag server dcutscenes.<[data.name]>:<[data]>
          - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
          - inventory open d:dcutscene_inventory_sub_keyframe
          - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
          - define text "Cutscene stop point set to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
          - narrate "<[msg_prefix]> <gray><[text]>"
          - ~run dcutscene_sort_data def.cutscene:<[data.name]>

      #-Remove stop scene keyframe
      - case remove:
        - define data.keyframes <[data.keyframes].exclude[stop]>
        - flag server dcutscenes.<[data.name]>:<[data]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
        - define text "Cutscene stop point on tick <green><[tick.tick]>t <gray>has been removed from scene <green><[data.name]><gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - inventory open d:dcutscene_inventory_sub_keyframe
        - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
        - ~run dcutscene_sort_data def.cutscene:<[data.name]>

#================  Camera Modifier =====================
dcutscene_cam_keyframe_edit:
    type: task
    debug: false
    definitions: option|arg|arg_2|arg_3
    script:
    - define option <[option]||null>
    - define arg <[arg]||null>
    - if <[option]> == null:
      - debug error "Something went wrong in dcutscene_cam_keyframe_edit could not determine option."
    - else:
      - define data <player.flag[cutscene_data]>
      - define camera_data <[data.keyframes.camera]||>
      - define tick <player.flag[dcutscene_tick_modify]>
      - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
      - choose <[option]>:
        #=========== New Camera ===========
        #-Prepare to create new keyframe modifier
        - case new:
          - define cam_check <[camera_data.<[tick]>]||null>
          - if <[cam_check]> != null:
            - define text "There is already a camera on this tick."
            - narrate "<[msg_prefix]> <gray><[text]>"
          - else:
            - flag <player> cutscene_modify:create_cam expire:120s
            - fakespawn dcutscene_camera_entity[equipment=[helmet=<item[dcutscene_camera_item]>]] <player.location> players:<player> d:10s save:camera
            - define camera <entry[camera].faked_entity>
            - flag <player> dcutscene_camera:<[camera]>
            - define text "Go to the location you'd like this camera to be at and chat <green>confirm<gray>."
            - narrate "<[msg_prefix]> <gray><[text]>"
            - adjust <player> gamemode:spectator
            - inventory close

        #-Create the camera keyframe modifier
        - case create:
          - flag <player> cutscene_modify:!
          - adjust <player> gamemode:creative
          - define camera <player.flag[dcutscene_camera]>
          - teleport <[camera]> <player.location>
          - define ray <player.eye_location.ray_trace[range=4;return=precise;default=air]>
          #data input
          - definemap eye_loc location:<[ray]> boolean:false
          - definemap cam_keyframe location:<player.location> rotate_mul:1.0 interpolation:linear move:true eye_loc:<[eye_loc]> tick:<[tick]> recording:false
          - look <[camera]> <[ray]> duration:2t
          - adjust <[camera]> armor_pose:[head=<player.location.pitch.to_radians>,0.0,0.0]
          #=-Debugger
          - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
            - ~run dcutscene_debugger def:camera_create|<[cam_keyframe]>
            - stop
          - define text "Camera location set to <green><player.location.simple> <gray>and look point <green><[ray].simple><gray> <gray>for keyframe tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
          - narrate "<[msg_prefix]> <gray><[text]>"
          - flag server dcutscenes.<[data.name]>.keyframes.camera.<[tick]>:<[cam_keyframe]>
          #Sort the newly created data
          - ~run dcutscene_sort_data def.cutscene:<[data.name]>
          #Update the player's cutscene data
          - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
          - inventory open d:dcutscene_inventory_sub_keyframe
          - ~run dcutscene_origin_set def.scene:<[data.name]>
          - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

        #-Teleport to camera location
        - case teleport:
          - define cam_loc <location[<[camera_data.<[tick]>.location]>]||null>
          - if <[cam_loc].equals[null]>:
            - debug error "Could not find location for camera in dcutscene_cam_keyframe_edit"
          - else:
            - teleport <player> <[cam_loc]>
            - define text "You have teleported to <green><[cam_loc].simple> <gray>at tick <green><[tick]>t<gray>."
            - narrate "<[msg_prefix]> <gray><[text]>"
            - inventory open d:dcutscene_inventory_keyframe_modify_camera
            - adjust <player> gamemode:creative

        #========= Edit the camera keyframe modifier =========
        - case edit:
            - choose <[arg]>:
              #-Preparation for new location in present camera keyframe
              - case new_location:
                - flag <player> cutscene_modify:create_present_cam expire:120s
                - fakespawn dcutscene_camera_entity[equipment=[helmet=<item[dcutscene_camera_item]>]] <player.location> players:<player> d:10s save:camera
                - define camera <entry[camera].faked_entity>
                - flag <player> dcutscene_camera:<[camera]>
                - define text "Go to the location you'd like this camera to be at and chat <green>confirm<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - adjust <player> gamemode:spectator
                - inventory close

              #-Create the new location in a present camera keyframe
              - case create_new_location:
                - flag <player> cutscene_modify:!
                - adjust <player> gamemode:creative
                - define camera <player.flag[dcutscene_camera]>
                - teleport <[camera]> <player.location>
                - define ray <player.eye_location.ray_trace[range=4;return=precise;default=air]>
                - look <[camera]> <[ray]> duration:2t
                - adjust <[camera]> armor_pose:[head=<player.location.pitch.to_radians>,0.0,0.0]
                #data input
                - define cam_keyframe <[camera_data.<[tick]>]>
                - define cam_keyframe.eye_loc.location <[ray]>
                - define cam_keyframe.location <player.location>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:camera_create_new_location|<[cam_keyframe]>
                  - stop
                #final data input
                - flag server dcutscenes.<[data.name]>.keyframes.camera.<[tick]>:<[cam_keyframe]>
                - define text "Camera location set to <green><player.location.simple> <gray>and look point <green><[ray].simple><gray> <gray>for keyframe tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                #Update the player's cutscene data
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_origin_set def.scene:<[data.name]>
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

              #-Prepare for new look location
              - case new_look_location:
                - flag <player> cutscene_modify:create_present_cam_look_loc expire:3m
                - define text "Available Inputs:"
                - narrate "<[msg_prefix]> <gray><[text]>"
                - narrate "<gray>Chat <green>confirm <gray>to input your location"
                - narrate "<gray>Chat a valid location tag"
                - narrate "<gray>Right click a block"
                - narrate "<gray>Chat <red>false <gray>to disable look location"
                - inventory close

              #-Set new look location for camera
              - case create_look_location:
                - if <[arg_2]> != false:
                  - define loc <location[<[arg_2].parsed>]||null>
                  - if <[loc]> == null:
                    - define text "<green><[arg_2]> <gray>is not a valid location."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - stop
                - else:
                  - define loc false
                - flag <player> cutscene_modify:!
                - define keyframes <[data.keyframes]>
                - define cam_keyframe <[keyframes.camera.<[tick]>]>
                - if <[loc]> != false:
                  - define cam_keyframe.eye_loc.location <[loc]>
                  - define cam_keyframe.eye_loc.boolean true
                - else:
                  - define cam_keyframe.eye_loc.boolean false
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:create_look_location|<[cam_keyframe]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.camera.<[tick]>:<[cam_keyframe]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define loc_msg <location[<[loc]>].simple||null>
                - if <[loc_msg]> == null:
                  - define loc_msg false
                - define text "Camera on tick <green><[tick]>t <gray>look location is now <green><[loc_msg]> <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_camera
                - ~run dcutscene_origin_set def.scene:<[data.name]>

              #-Change path interpolation method
              - case interpolation_change:
                - define item <item[<[arg_2]>]>
                - define cam_keyframe <[camera_data.<[tick]>]>
                - define interp_method <[cam_keyframe.interpolation]>
                - choose <[interp_method]>:
                  - case linear:
                    - define new_interp_method smooth
                  - case smooth:
                    - define new_interp_method linear
                - define cam_keyframe.interpolation <[new_interp_method]>
                - flag server dcutscenes.<[data.name]>.keyframes.camera.<[tick]>:<[cam_keyframe]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define interp_msg "<green><bold>Interpolation: <gray><[new_interp_method].to_uppercase>"
                - define click "<gray><italic>Click to modify interpolation method"
                - define lore <list[<empty>|<[interp_msg]>|<empty>|<[click]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_3]> lore:<[lore]>

              #-Change if the camera will move to the next point
              - case move_change:
                - define item <item[<[arg_2]>]>
                - define cam_keyframe <[camera_data.<[tick]>]>
                - define move <[cam_keyframe.move]||true>
                - choose <[move]>:
                  - case true:
                    - define move false
                  - case false:
                    - define move true
                - define cam_keyframe.move <[move]>
                - flag server dcutscenes.<[data.name]>.keyframes.camera.<[tick]>:<[cam_keyframe]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define info_msg "<gray>Determine if the camera will move to the next keyframe point"
                - define interp_msg "<green><bold>Move: <gray><[move]>"
                - define click "<gray><italic>Click to modify movement for camera"
                - define lore <list[<empty>|<[info_msg]>|<empty>|<[interp_msg]>|<empty>|<[click]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_3]> lore:<[lore]>

              #-Determine if camera look is inverted
              - case invert_camera:
                - define item <item[<[arg_2]>]>
                - define invert <[camera_data.<[tick]>.invert]||false>
                - choose <[invert]>:
                  - case true:
                    - define invert false
                  - case false:
                    - define invert true
                - define camera_data.<[tick]>.invert <[invert]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:invert_camera|<[camera_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.camera:<[camera_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define info_msg "<dark_purple>Invert: <gray><[invert]>"
                - define click "<gray><italic>Click to change invert look"
                - define lore <list[<empty>|<[info_msg]>|<empty>|<[click]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_3]> lore:<[lore]>

              #-Determine if the camera will interpolate the look rotation
              - case interpolate_look:
                - define item <item[<[arg_2]>]>
                - define data <player.flag[cutscene_data]>
                - define keyframes <[data.keyframes]>
                - define cam_keyframe <[keyframes.camera.<[tick]>]>
                - define interp_look <[cam_keyframe.interpolate_look]||true>
                - choose <[interp_look]>:
                  - case true:
                    - define new_interp false
                  - case false:
                    - define new_interp true
                - define cam_keyframe.interpolate_look <[new_interp]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:interpolate_look|<[cam_keyframe]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.camera.<[tick]>:<[cam_keyframe]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define info_msg "<gray>Determine if the camera will interpolate to the next look point."
                - define interp_msg "<green><bold>Interpolate Look: <gray><[new_interp]>"
                - define click "<gray><italic>Click to modify look interpolation for camera"
                - define lore <list[<empty>|<[info_msg]>|<empty>|<[interp_msg]>|<empty>|<[click]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_3]> lore:<[lore]>

              #-Determine if camera will rotate and look
              - case rotate_change:
                - choose <[arg_2]>:
                  #Prepare for new rotation multipler
                  - case new_mul:
                    - flag <player> cutscene_modify:new_rotation_mul expire:2m
                    - define text "Chat the rotate multiplier the default value is <green>1<gray>. To cancel this chat <red>cancel<gray>."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - inventory close
                  #Set the new rotation multipler
                  - case set_mul:
                    - if !<[arg_3].is_decimal>:
                      - define text "<green><[arg_3]> <gray>is not a valid number."
                      - narrate "<[msg_prefix]> <gray><[text]>"
                      - stop
                    - flag <player> cutscene_modify:!
                    - define cam_keyframe <[camera_data.<[tick]>]>
                    - define cam_keyframe.rotate_mul <[arg_3]>
                    #=-Debugger
                    - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                      - ~run dcutscene_debugger def:rotate_change|<[cam_keyframe]>
                      - stop
                    - flag server dcutscenes.<[data.name]>.keyframes.camera.<[tick]>:<[cam_keyframe]>
                    - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                    - define text "<gray>Camera in tick <green><[tick]>t <gray>rotate multiplier is now <green><[arg_3]> <gray>for scene <green><[data.name]><gray>."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - inventory open d:dcutscene_inventory_keyframe_modify_camera

              #-Record camera preparation
              - case record_camera_prep:
                #Check if there is a keyframe in the way
                - foreach <[camera_data]> key:c_tick as:cam_data:
                  - if <[cam_data.tick]> > <[tick]>:
                    - define keyframe_check <[cam_data]>
                    - foreach stop
                - flag <player> cutscene_modify:camera_record_false expire:4m
                #If there is a camera animator in the way
                - if <[keyframe_check]||null> != null:
                  - define key_time <[keyframe_check.tick]>
                  - define text "There is a camera animator at tick <green><[key_time]>t<gray>."
                  - define calc_time <[key_time].sub[<[tick]>]>t
                  - clickable dcutscene_cam_keyframe_edit def:edit|record_camera_begin|default usages:1 save:default
                  - clickable dcutscene_cam_keyframe_edit def:edit|record_camera_duration_chat usages:1 save:own
                  - define prefix <[msg_prefix]>
                  - define click_default "<element[<green><bold>Use duration <[calc_time]>].on_hover[<[prefix]> <gray>Use the duration calculated.].type[SHOW_TEXT].on_click[<entry[default].command>]>"
                  - define click_own "<element[<aqua><bold>Use my own].on_hover[<[prefix]> <gray>Chat your own duration (Must be less than duration <[calc_time]>).].type[SHOW_TEXT].on_click[<entry[own].command>]>"
                  - define text_2 "<gray>Would you like to use the duration <green><[calc_time]> <gray>or specify your own duration?  <[click_default]>  <[click_own]>"
                  - define text_3 "<gray>Note that if you specify your own duration it must be less than the time duration <green><[calc_time]><gray>."
                  - define text_4 "<gray>Chat <red>false <gray>for no camera recording."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - narrate <[text_2]>
                  - narrate <[text_3]>
                  - narrate <[text_4]>
                  - definemap record_data tick:<[tick]> key_time:<[calc_time]>
                  - flag <player> dcutscene_save_data.record_data:<[record_data]>
                #No keyframe animator
                - else:
                  - definemap record_data tick:<[tick]> key_time:false
                  - flag <player> dcutscene_save_data.record_data:<[record_data]>
                  - run dcutscene_cam_keyframe_edit def:edit|record_camera_duration_chat
                - inventory close

              #-Set the duration of the camera recorder
              - case record_camera_duration_chat:
                - define text "Chat the duration the camera will record your movement. Chat <red>false <gray>for no camera recording."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:camera_recorder_duration expire:4m

              #-Set camera recorder to false
              - case record_camera_false:
                - define tick <player.flag[dcutscene_save_data.record_data.tick]>
                - define camera_data.<[tick]>.recording.bool:false
                - flag server dcutscenes.<[data.name]>.keyframes.camera:<[camera_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define text "Camera animator at tick <green><[tick]>t<gray> recording is now <red>false<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_camera

              #-Record and set the recorded data
              - case record_camera_begin:
                - define max_time <player.flag[dcutscene_save_data.record_data.key_time]>
                - define tick <player.flag[dcutscene_save_data.record_data.tick]>
                - choose <[arg_2]>:
                  - case default:
                    - define create_new_keyframe false
                    - define duration <duration[<[max_time]>]>
                    - define end_tick <[max_time]>
                  - case own:
                    - define create_new_keyframe true
                    - if !<[arg_3]>:
                      - run dcutscene_cam_keyframe_edit def:edit|record_camera_false
                      - stop
                    - define duration <duration[<[arg_3]>]||null>
                    - if <[duration]> == null:
                      - define text "<green><[duration]> <gray>is not a valid duration"
                      - narrate "<[msg_prefix]> <gray><[text]>"
                      - stop
                    - if <[max_time]> != false:
                      - if <[duration].in_ticks> > <duration[<[max_time]>].in_ticks>:
                        - define text "The duration cannot be more than <green><[max_time]><gray>."
                        - narrate "<[msg_prefix]> <gray><[text]>"
                        - stop
                - if <[duration]> == null:
                  - define text "Invalid duration specified."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - stop
                - flag <player> cutscene_modify:!
                #If there is previous recording data
                - if <[camera_data.<[tick]>.recording]> != false:
                  - define camera_data.<[tick]>.recording:<empty>
                #Data
                - define text "<gray>Recording will begin in..."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - narrate <green><bold>5
                - wait 1s
                - narrate <green><bold>4
                - wait 1s
                - narrate <green><bold>3
                - wait 1s
                - narrate <green><bold>2
                - wait 1s
                - narrate <green><bold>1
                - wait 1s
                - narrate "<[msg_prefix]> <gray>Recording has begun."
                #l = location
                #e = eye_location
                #y = yaw
                #p = pitch
                - define length <[duration].in_ticks>
                - repeat <[length]> as:tick-i:
                  - define pl <player.location>
                  - define loc <location[<[pl].x.round_up_to_precision[0.005]>,<[pl].y.round_up_to_precision[0.005]>,<[pl].z.round_up_to_precision[0.005]>]>
                  - define e <player.eye_location>
                  - define y <[e].yaw.round_up_to_precision[0.005]>
                  - define p <[e].pitch.round_up_to_precision[0.005]>
                  - definemap rec_data l:<[loc]> y:<[y]> p:<[p]>
                  - define camera_data.<[tick]>.recording.frames.<[tick-i]>:<[rec_data]>
                  - wait 1t
                  #Input new keyframe if duration was specified by the creator
                  - if <[tick-i]> == <[length]> && <[create_new_keyframe].if_null[false].is_truthy>:
                    - definemap eye_loc location:<[e]> boolean:false
                    - define new_tick <[tick].add[<[tick-i]>]>
                    - definemap cam_keyframe location:<[pl]> rotate_mul:1.0 interpolation:linear move:true eye_loc:<[eye_loc]> tick:<[new_tick]> recording:false
                    - define camera_data.<[new_tick]>:<[cam_keyframe]>
                    - define end_tick <[new_tick]>t
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:camera_record|<[camera_data]>
                  - stop
                #Sort and data set
                - define camera_data.<[tick]>.recording.frames <[camera_data.<[tick]>.recording.frames].sort_by_value[get[tick]]>
                - define highest <[camera_data.<[tick]>.recording.frames].keys.highest>
                - define camera_data.<[tick]>.recording.length:<[highest]>
                - define camera_data.<[tick]>.recording.bool:true
                #Data set
                - flag server dcutscenes.<[data.name]>.keyframes.camera:<[camera_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - define text "Camera recording is finished and has been set to tick <green><[tick]>t <gray>with the ending tick at tick <green><[end_tick]> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_camera
                - ~run dcutscene_origin_set def.scene:<[data.name]>

              #-Move camera animator to new keyframe preparation
              - case move_camera_prep:
                - define camera <[camera_data.<[tick]>]>
                - definemap move_data animator:camera type:move tick:<[tick]> data:<[camera]> scene:<[data.name]>
                - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
                - define text "Click on the tick you'd like to move this camera animator to. To stop chat <red>cancel<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - ~run dcutscene_keyframe_modify def:back

              #-Move the camera to a new keyframe
              - case move_camera:
                - define move_data <player.flag[dcutscene_animator_change]>
                #Remove Camera
                - define data.keyframes.camera <[data.keyframes.camera].deep_exclude[<[move_data.tick]>]>
                #Set previous camera to new tick
                - define data.keyframes.camera.<[tick]>:<[move_data.data]>
                #Update tick on camera
                - define data.keyframes.camera.<[tick]>.tick <[tick]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:camera_move|<[data]>
                  - stop
                - flag server dcutscenes.<[data.name]>:<[data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_origin_set def.scene:<[data.name]>
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

              #-Duplicate camera to a new keyframe preperation
              - case duplicate_camera_prep:
                - define camera <[camera_data.<[tick]>]>
                - definemap dup_data animator:camera type:duplicate tick:<[tick]> data:<[camera]> scene:<[data.name]>
                - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
                - define text "Click on the tick you'd like to duplicate this camera animator to. To stop chat <red>cancel<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - ~run dcutscene_keyframe_modify def:back

              #-Duplicate camera
              - case duplicate_camera:
                - define dup_data <player.flag[dcutscene_animator_change]>
                #Set the camera
                - define data.keyframes.camera.<[tick]>:<[dup_data.data]>
                #Update tick on camera
                - define data.keyframes.camera.<[tick]>.tick <[tick]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:camera_duplicate|<[data]>
                  - stop
                - flag server dcutscenes.<[data.name]>:<[data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_origin_set def.scene:<[data.name]>
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[dup_data.animator]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

              #-Playing the cutscene from this tick (The tick needs to be subtracted by 1 due to how the animator functions)
              - case play_from_here:
                - inventory close
                - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
                - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

              #-Remove camera from keyframe
              - case remove_camera:
                - define cam_keyframe <[data.keyframes.camera].deep_exclude[<[tick]>]>
                - define data.keyframes.camera:<[cam_keyframe]>
                - if <[data.keyframes.camera].is_empty>:
                  - define data.keyframes <[data.keyframes].deep_exclude[camera]>
                - flag server dcutscenes.<[data.name]>:<[data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define text "Camera from tick <green><[tick]>t <gray>has been removed for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

#========== Models and Entity Modifiers ============

#Used to modify models or entities in keyframes

#This uses a different type of data compared to the other animators for instance there are root models and sub-frame models
#let's say you change the id of the root model or sub-frame model this will update every tick that contains the model.
#While albeit this is a bit more complicated internally this makes the model changing process much simpler for the creator.
#This data is similar to a chain essentially.
#The root model is the starting point that contains the path and sub-frames with ticks and UUIDs.
#For a visual example on how this data appears make your model with sub frames, turn off save file compression, save your cutscene, and look at the json file created.

#Removes the previous spawned model the player was modifying
dcutscene_model_remove:
    type: task
    debug: false
    definitions: type|root_ent
    script:
    - if <[root_ent].is_spawned>:
      - choose <[type]>:
        - case player_model:
            - run pmodels_remove_model def:<[root_ent]>
        - case model:
            - run dmodels_delete def:<[root_ent]>

dcutscene_model_keyframe_edit:
    type: task
    debug: false
    definitions: option|arg|arg_2|arg_3
    script:
    - define option <[option]||null>
    - if <[option]> == null:
      - debug error "Something went wrong in dcutscene_model_keyframe_edit could not determine option"
    - else:
      - define data <player.flag[cutscene_data]>
      - define tick <player.flag[dcutscene_tick_modify.tick].if_null[<player.flag[dcutscene_tick_modify]>]>
      - define uuid <player.flag[dcutscene_tick_modify.uuid]||>
      - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
      - choose <[option]>:
        #========= Denizen Models Modifier =========

        - case denizen_model:
          #-Check if creator has DModels
          - if <script[dmodels_spawn_model]||null> == null:
            - debug error "Could not find Denizen Models in dcutscene_model_keyframe_edit"
            - define text "Could not find Denizen Models download it from one of these links."
            - define text_2 "<gray>Forums: <green><&click[https://forum.denizenscript.com/resources/denizen-models.103/].type[OPEN_URL]>click here<&end_click><gray>."
            - define text_3 "<gray>Github: <green><&click[https://github.com/mcmonkeyprojects/DenizenModels].type[OPEN_URL]>click here<&end_click><gray>."
            - narrate "<[msg_prefix]> <gray><[text]>"
            - narrate <[text_2]>
            - narrate <[text_3]>
            - inventory close
          - else:
            - choose <[arg]>:
              #-New model preparation
              - case new:
                - run dcutscene_model_list_new def.tick:<[tick]> def.scene:<[data.name]> def.type:model

              #-Create new model ID
              - case create_id:
                #Check if model has already been set in tick
                - define model_list <[data.keyframes.models.<[tick]>.model_list]||<list>>
                - foreach <[model_list]> as:model_uuid:
                  - define model_id <[data.keyframes.models.<[tick]>.<[model_uuid]>.id]>
                  - if <[model_id]> == <[arg_2]>:
                    - define model_type <[data.keyframes.models.<[tick]>.<[model_uuid]>.type]>
                    - define text "There is already a <[model_type]> with an id of <green><[arg_2]> <gray>in tick <green><[tick]>t<gray>."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - stop
                #Save data for continuous data input in modifiers
                - flag <player> dcutscene_save_data.id:<[arg_2]>
                - define text "Use the command /dcutscene model <green>my_model <gray>to set the model."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:set_model_name expire:3m

              #-Set the model and give the location tool to set the location
              - case create_model_name:
                - define model_id <player.flag[dcutscene_save_data.id]||null>
                - if <[model_id]> == null:
                  - debug error "Something went wrong could not determine model ID in dcutscene_model_keyframe_edit for create_model_name"
                  - stop
                #Model verification
                - if <server.has_flag[dmodels_data]>:
                  - if <server.flag[dmodels_data.model_<[arg_2]>]||null> != null:
                    #If there is a model present remove it
                    - if <player.has_flag[dcutscene_location_editor]>:
                      - define loc_data <player.flag[dcutscene_location_editor]>
                      - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                    #Give location tool and spawn the model
                    - flag <player> cutscene_modify:new_model_location expire:10m
                    - run dmodels_spawn_model def.model_name:<[arg_2]> def.location:<player.location> def.tracking_range:256 def.fake_to:<player> save:spawned
                    - define root <entry[spawned].created_queue.determination.first>
                    #Don't reset save_data with a - definemap
                    - flag <player> dcutscene_save_data.root:<[root]>
                    - flag <player> dcutscene_save_data.model:<[arg_2]>
                    - run dcutscene_location_tool_give_data def:<player.location>|<[root]>|<[root].location.yaw>|model|<[arg_2]>
                    - define text "After choosing your location for this model click <green>Confirm Location <gray>in the location GUI or chat <green>confirm<gray>. To re-open the location GUI do /dcutscene location."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - inventory open d:dcutscene_inventory_location_tool
                  - else:
                    - define text "That model does not seem to exist."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                - else:
                  - define text "There is no model data available"
                  - narrate "<[msg_prefix]> <gray><[text]>"

              #-Set the model into the data and location
              - case location_set_and_create_model:
                - if <location[<[arg_2]>]||null> == null:
                  - define text "<green><[arg_2]> <gray>is an invalid location this isn't supposed to happen for this part...either I'm a really bad coder or ya done fucked up."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - debug error "Invalid location in dcutscene_model_keyframe_edit for location_set_and_create_model"
                  - stop
                - flag <player> cutscene_modify:!
                - define model_uuid <util.random_uuid>
                - define save_data <player.flag[dcutscene_save_data]>
                #Model data
                - definemap model_data id:<[save_data.id]> model:<[save_data.model]> type:model root:none sub_frames:none item:<item[dcutscene_model_keyframe_item]||<item[dragon_head]>>
                #Set model to path data
                - definemap ray_trace_data direction:floor liquid:false passable:false
                - definemap path_data interpolation:linear rotate_interp:true rotate_mul:1.0 move:true location:<[arg_2]> animation:false ray_trace:<[ray_trace_data]> tick:<[tick]>
                - define model_data.path.<[tick]> <[path_data]>
                - define data.keyframes.models.<[tick]>.<[model_uuid]> <[model_data]>
                - define data.keyframes.models.<[tick]>.model_list:->:<[model_uuid]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:model_create|<[data.keyframes.models]>
                  - stop
                - flag server dcutscenes.<[data.name]>:<[data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define text "Denizen Model <green><[save_data.model]> <gray>has been created for tick <green><[tick]>t <gray>with an ID of <green><[save_data.id]> <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                #Remove created player model
                - if <player.has_flag[dcutscene_location_editor]>:
                  - define loc_data <player.flag[dcutscene_location_editor]>
                  - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                - run dcutscene_location_tool_return_inv
                - flag <player> dcutscene_location_editor:!
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

              #-Create new keyframe point for presently created model
              - case create_present:
                - choose <[arg_2]>:
                  #Prepare for new keyframe
                  - case new_keyframe_prepare:
                    #Data from items in the models GUI
                    - define root_save <[arg_3]||null>
                    - if <[root_save]> != null:
                      #Get the starting point model data
                      - define root_tick <[root_save.tick]>
                      - define root_uuid <[root_save.uuid]>
                      - define root_data <[data.keyframes.models.<[root_tick]>.<[root_uuid]>]||null>
                      - if <[root_data]> == null:
                        - debug error "Something went wrong in dcutscene_model_keyframe_edit could not find root model keyframe"
                      - else:
                        - flag <player> cutscene_modify:new_model_keyframe_point
                        #If there is a present player model remove it
                        - if <player.has_flag[dcutscene_location_editor]>:
                          - define loc_data <player.flag[dcutscene_location_editor]>
                          - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                        - define text "After choosing your location for this model click <green>Confirm Location <gray>in the location GUI or chat <green>confirm<gray>. To re-open the location GUI do /dcutscene location."
                        - narrate "<[msg_prefix]> <gray><[text]>"
                        - flag <player> dcutscene_save_data.data:<[root_save]>
                        - define model <[root_data.model]>
                        - run dmodels_spawn_model def:<[model]>|<player.location>|256|<player> save:spawned
                        - define root <entry[spawned].created_queue.determination.first>
                        - flag <player> dcutscene_save_data.root:<[root]>
                        - run dcutscene_location_tool_give_data def:<player.location>|<[root]>|<[root].location.yaw>|model|<[model]>
                        - inventory open d:dcutscene_inventory_location_tool

                  #Set the new keyframe point
                  - case new_keyframe_set:
                    - flag <player> cutscene_modify:!
                    - define loc <location[<[arg_3]>]>
                    #Root Model Data
                    - define root_save <player.flag[dcutscene_save_data.data]||null>
                    - define root_tick <[root_save.tick]>
                    - define root_uuid <[root_save.uuid]>
                    - define root_id <[root_save.id]>
                    #Default data for new model
                    - definemap ray_trace_data direction:floor liquid:false passable:false
                    - definemap path_data rotate_interp:true rotate_mul:1.0 interpolation:linear location:<[loc]> move:true animation:false ray_trace:<[ray_trace_data]> tick:<[tick]>
                    #Update the root data
                    - define path <[data.keyframes.models.<[root_tick]>.<[root_uuid]>.path]>
                    - define path.<[tick]> <[path_data]>
                    #Update root model
                    - flag server dcutscenes.<[data.name]>.keyframes.models.<[root_tick]>.<[root_uuid]>.path:<[path]>
                    - flag server dcutscenes.<[data.name]>.keyframes.models.<[root_tick]>.<[root_uuid]>.sub_frames.<[tick]>:<[root_uuid]>
                    #Set new model
                    - define model_uuid <[root_uuid]>
                    - definemap model_data id:<[root_id]> type:model path:false
                    - define model_data.root.tick <[root_tick]>
                    - define model_data.root.uuid <[root_uuid]>
                    #=-Debugger
                    - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                      - ~run dcutscene_debugger def:model_new_keyframe_set|<[model_data]>
                      - stop
                    - flag server dcutscenes.<[data.name]>.keyframes.models.<[tick]>.<[model_uuid]>:<[model_data]>
                    - flag server dcutscenes.<[data.name]>.keyframes.models.<[tick]>.model_list:->:<[model_uuid]>
                    - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                    #Remove created player model
                    - if <player.has_flag[dcutscene_location_editor]>:
                      - define loc_data <player.flag[dcutscene_location_editor]>
                      - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                    - run dcutscene_location_tool_return_inv
                    #Sort ticks by time
                    - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                    - flag <player> dcutscene_location_editor:!
                    - inventory open d:dcutscene_inventory_sub_keyframe
                    - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                    - define text "Model <green><[model_data.id]> <gray>has been set to tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                    - narrate "<[msg_prefix]> <gray><[text]>"

              #-Change model ID preparation
              - case change_id_prep:
                - define text "Chat the new ID for this model."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:change_model_id expire:3m
                - inventory close

              #-Change model ID
              - case change_id:
                - flag <player> cutscene_modify:!
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                #If there is a root update the root and all sub frames
                - if <[root_data]> != none:
                  - define model_data.<[root_data.tick]>.<[root_data.uuid]>.id <[arg_2]>
                  - define sub_frames <[model_data.<[root_data.tick]>.<[root_data.uuid]>.sub_frames]||none>
                  - if <[sub_frames]> != none:
                    - foreach <[sub_frames]> key:frame_tick as:frame_uuid:
                      - define model_data.<[frame_tick]>.<[frame_uuid]>.id <[arg_2]>
                #If the model is a root data model update the sub frames should they exist
                - else:
                  - define model_data.<[tick]>.<[uuid]>.id <[arg_2]>
                  - define sub_frames <[model_data.<[tick]>.<[uuid]>.sub_frames]||none>
                  - if <[sub_frames]> != none:
                    - foreach <[sub_frames]> key:frame_tick as:frame_uuid:
                      - define model_data.<[frame_tick]>.<[frame_uuid]>.id <[arg_2]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:model_set_ID|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define text "Model ID changed to <green><[arg_2]> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_model

              #-Change model item in GUI preparation
              - case change_model_item_prep:
                - define text "Chat a valid item tag or chat <green>hand <gray>for the item in your hand."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:change_model_item expire:5m
                - inventory close

              #-Change model item
              - case change_model_item:
                - if <item[<[arg_2]>]||null> == null && <item[<[arg_2]>].material.name> == air:
                  - define text "<green><[arg_2]> <gray>is not a valid item."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - stop
                - else:
                  - adjust <item[<[arg_2]>]> quantity:1 save:item
                  - define item <entry[item].result>
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                - if <[root_data]> != none:
                  - define model_data.<[root_data.tick]>.<[root_data.uuid]>.item <[item]>
                - else:
                  - define model_data.<[tick]>.<[uuid]>.item <[item]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:model_set_item|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define text "Model GUI item changed to <green><[item]> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_model

              #-Change model location prep
              - case change_location_prep:
                #If there is a present player model remove it
                - if <player.has_flag[dcutscene_location_editor]>:
                  - define loc_data <player.flag[dcutscene_location_editor]>
                  - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                - define model_data  <[data.keyframes.models]>
                - define root_save <[model_data.<[tick]>.<[uuid]>.root]||none>
                - if <[root_save]> == none:
                  - define root_save.tick <[tick]>
                  - define root_save.uuid <[uuid]>
                  - define model <[model_data.<[tick]>.<[uuid]>.model]>
                - else:
                  - define model <[model_data.<[root_save.tick]>.<[root_save.uuid]>.model]>
                - define text "After choosing your location for this model click <green>Confirm Location <gray>in the location GUI or chat <green>confirm<gray>. To re-open the location GUI do /dcutscene location."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:set_new_model_location
                - flag <player> dcutscene_save_data.data:<[root_save]>
                - flag <player> dcutscnee_save_data.model:<[model]>
                - run dmodels_spawn_model def:<[model]>|<player.location>|256|<player> save:spawned
                - define root <entry[spawned].created_queue.determination.first>
                - flag <player> dcutscene_save_data.root:<[root]>
                - run dcutscene_location_tool_give_data def:<player.location>|<[root]>|<[root].location.yaw>|model|<[model]>
                - inventory open d:dcutscene_inventory_location_tool

              #-Change model location
              - case change_location:
                - flag <player> cutscene_modify:!
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                - if <[root_data]> != none:
                  - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.location <[arg_2]>
                - else:
                  - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.location <[arg_2]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:model_set_location|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define text "Model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>location is now <green><[arg_2].simple> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_model
                #If there is a model present remove it
                - if <player.has_flag[dcutscene_location_editor]>:
                  - define loc_data <player.flag[dcutscene_location_editor]>
                  - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                - run dcutscene_location_tool_return_inv
                - flag <player> dcutscene_location_editor:!

              #-Ray Trace Modifying
              - case ray_trace:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                #Ray Trace Data
                - if <[root_data]> != none:
                  - define tick_modify <[root_data.tick]>
                  - define uuid_modify <[root_data.uuid]>
                  - define ray_trace <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.ray_trace]>
                - else:
                  - define tick_modify <[tick]>
                  - define uuid_modify <[uuid]>
                  - define ray_trace <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.ray_trace]>
                - choose <[arg_2]>:
                  #Ray Trace Direction
                  - case ray_trace_direction:
                    - define direction <[ray_trace.direction]||floor>
                    - choose <[direction]>:
                      - case false:
                        - define direction floor
                      - case floor:
                        - define direction ceiling
                      - case ceiling:
                        - define direction false
                    - define l1 "<red>Direction: <gray><[direction]>"
                    - define l2 "<gray><italic>Click to change ray trace direction"
                    - define model_data.<[tick_modify]>.<[uuid_modify]>.path.<[tick]>.ray_trace.direction:<[direction]>
                  #Ray Trace Liquid
                  - case ray_trace_liquid:
                    - define liquid <[ray_trace.liquid].if_null[false]>
                    - choose <[liquid]>:
                      - case false:
                        - define liquid true
                      - case true:
                        - define liquid false
                    - define l1 "<blue>Liquid: <gray><[liquid]>"
                    - define l2 "<gray><italic>Click to change ray trace liquid"
                    - define model_data.<[tick_modify]>.<[uuid_modify]>.path.<[tick]>.ray_trace.liquid:<[liquid]>
                  #Ray Trace Passable
                  - case ray_trace_passable:
                    - define passable <[ray_trace.passable].if_null[false]>
                    - choose <[passable]>:
                      - case false:
                        - define passable true
                      - case true:
                        - define passable false
                    - define l1 "<green>Passable: <gray><[passable]>"
                    - define l2 "<gray><italic>Click to change ray trace passable"
                    - define model_data.<[tick_modify]>.<[uuid_modify]>.path.<[tick]>.ray_trace.passable:<[passable]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:model_ray_trace|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_3]> lore:<[lore]>

              #-Change model prep
              - case change_model_prep:
                - define root_data <[data.keyframes.models.<[tick]>.<[uuid]>.root]||none>
                - define text "Use /dcutscene model <green>my_model_name <gray>to change the model."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:change_model_prep expire:2m
                - inventory close

              #-Change model
              - case change_model:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                - if <[root_data]> != none:
                  - define model_data.<[root_data.tick]>.<[root_data.uuid]>.model <[arg_2]>
                - else:
                  - define model_data.<[tick]>.<[uuid]>.model <[arg_2]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:change_model|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define text "Model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>will now spawn with model <green><[arg_2]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_model

              #-Whether the model will move to the next keyframe point
              - case set_move:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                #If the model contains a root data model
                - if <[root_data]> != none:
                  - define move <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.move]>
                  - choose <[move]>:
                    - case true:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.move false
                      - define move false
                    - case false:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.move true
                      - define move true
                #If the model is a root model
                - else:
                  - define move <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.move]>
                  - choose <[move]>:
                    - case true:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.move false
                      - define move false
                    - case false:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.move true
                      - define move true
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:model_set_move|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define l1 "<blue>Move: <gray><[move]>"
                - define l2 "<gray><italic>Click to change model move"
                - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

              #-Prepare for new animation
              - case new_animation_prepare:
                - define root_data <[data.keyframes.models.<[tick]>.<[uuid]>.root]||none>
                - if <[root_data]> != none:
                  - define model <[data.keyframes.models.<[root_data.tick]>.<[root_data.uuid]>.model]>
                - else:
                  - define model <[data.keyframes.models.<[tick]>.<[uuid]>.model]>
                - flag <player> cutscene_modify:set_model_animation
                - flag <player> dcutscene_save_data.type:model
                - flag <player> dcutscene_save_data.model:<[model]>
                - define text "To set the animation use the command /dcutscene animate <green>my_animation <gray>to prevent an animation from playing put <red>false<gray> to stop an animation from playing put <red>stop<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory close

              #-Set the animation for the player model keyframe point
              - case set_animation:
                - flag <player> cutscene_modify:!
                - define model_data <[data.keyframes.models]>
                - define root_data <[data.keyframes.models.<[tick]>.<[uuid]>.root]||none>
                #If the model contains a root data model
                - if <[root_data]> != none:
                  - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.animation <[arg_2]>
                #If the model is a root model
                - else:
                  - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.animation <[arg_2]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:model_set_animation|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - if <[arg_2]> != stop:
                  - define text "Model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>animation is now <green><[arg_2]> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - else:
                  - define text "Model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>animation will now <red><[arg_2]> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_model

              #-Set the path interpolation method
              - case change_path_interp:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                #If the model contains a root data model
                - if <[root_data]> != none:
                  - define interp <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.interpolation]>
                  - choose <[interp]>:
                    - case linear:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.interpolation smooth
                      - define interp smooth
                    - case smooth:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.interpolation linear
                      - define interp linear
                #If the model is a root data model
                - else:
                  - define interp <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.interpolation]>
                  - choose <[interp]>:
                    - case linear:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.interpolation smooth
                      - define interp smooth
                    - case smooth:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.interpolation linear
                      - define interp linear
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:model_path_interp|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define l1 "<yellow>Interpolation: <gray><[interp]>"
                - define l2 "<gray><italic>Click to change path interpolation method"
                - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

              #-Change rotation interpolate
              - case change_rotate_interp:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                #If the model contains a root data model
                - if <[root_data]> != none:
                  - define interp <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.rotate_interp].if_null[false]>
                  - choose <[interp]>:
                    - case true:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.rotate_interp false
                      - define interp false
                    - case false:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.rotate_interp true
                      - define interp true
                #If the model is a root data model
                - else:
                  - define interp <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.rotate_interp].if_null[false]>
                  - choose <[interp]>:
                    - case true:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.rotate_interp false
                      - define interp false
                    - case false:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.rotate_interp true
                      - define interp true
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:model_change_rotate_interp|<[model_data]>
                  - stop
                #Update data
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define l1 "<dark_purple>Rotate interpolation: <gray><[interp]>"
                - define l2 "<gray><italic>Click to change path rotation interpolation"
                - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

              #-Change rotate mul prep
              - case change_rotate_mul_prep:
                - define text "Chat a valid number for the rotation multiplier."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:model_change_rotate_mul expire:2m
                - inventory close

              #-Change rotate mul
              - case change_rotate_mul:
                - if <[arg_2].is_decimal>:
                  - flag <player> cutscene_modify:!
                  - define model_data <[data.keyframes.models]>
                  - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                  #If the model contains a root data model
                  - if <[root_data]> != none:
                    - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.rotate_mul <[arg_2]>
                  #If the model is a root data model
                  - else:
                    - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.rotate_mul <[arg_2]>
                  #=-Debugger
                  - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                    - ~run dcutscene_debugger def:model_rotate_mul|<[model_data]>
                    - stop
                  #Update data
                  - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                  - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                  - define text "Model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>rotation multiplier is now <green><[arg_2]> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - inventory open d:dcutscene_inventory_keyframe_modify_model
                - else:
                  - define text "<green><[arg_2]> <gray>is not a valid number."
                  - narrate "<[msg_prefix]> <gray><[text]>"

              #-Teleport to model location
              - case teleport_to:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                - if <[root_data]> != none:
                  - define tick_mod <[root_data.tick]>
                  - define tick_uuid <[root_data.uuid]>
                - else:
                  - define tick_mod <[tick]>
                  - define tick_uuid <[uuid]>
                - define loc <[model_data.<[tick_mod]>.<[tick_uuid]>.path.<[tick]>.location]||null>
                - if <[loc]> == null:
                  - debug error "Could not find location to teleport to in dcutscene_model_keyframe_edit for denizen model"
                - else:
                  - teleport <player> <location[<[loc]>].with_pitch[<player.location.pitch>]>
                  - define text "You have teleported to model <green><[model_data.<[tick_mod]>.<[tick_uuid]>.id]> <gray>location in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - inventory open d:dcutscene_inventory_keyframe_modify_model

              #-Case move to prep
              - case move_to_prep:
                - define model_data <[data.keyframes.models.<[tick]>.<[uuid]>]>
                - definemap move_data animator:model type:move tick:<[tick]> uuid:<[uuid]> data:<[model_data]> scene:<[data.name]>
                - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
                - define text "Click on the tick you'd like to move this model animator to. To stop chat <red>cancel<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - ~run dcutscene_keyframe_modify def:back

              #-Move to
              - case move_to:
                - define move_data <player.flag[dcutscene_animator_change]>
                - ~run dcutscene_move_model_animator save:result
                - if <entry[result].created_queue.determination.first> == valid:
                  - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"

              #-Duplicate prep
              - case duplicate_prep:
                - define player_model_data <[data.keyframes.models.<[tick]>.<[uuid]>]>
                - definemap dup_data animator:player_model type:duplicate tick:<[tick]> uuid:<[uuid]> data:<[player_model_data]> scene:<[data.name]>
                - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
                - define text "Click on the tick you'd like to duplicate this model animator to. To stop chat <red>cancel<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - ~run dcutscene_keyframe_modify def:back

              #-Duplicate
              - case duplicate:
                - define dup_data <player.flag[dcutscene_animator_change]>
                - ~run dcutscene_duplicate_model_animator save:result
                - if <entry[result].created_queue.determination.first> == valid:
                  - define text "Animator <green><[dup_data.animator]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"

              #-Play the cutscene from this tick
              - case play_from_here:
                - inventory close
                - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
                - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

              #-Removes model from tick
              - case remove_tick:
                - run dcutscene_remove_model_animator_from_tick def.type:model

              #-Removes model from entire cutscene
              - case remove_all:
                - run dcutscene_remove_model_animator_all def.type:model

        #======== Denizen Player Models Modifier =========

        - case player_model:
          #-If player model script does not exist
          - if <script[pmodels_spawn_model]||null> == null:
            - debug error "Could not find Denizen Player Models in dcutscene_model_keyframe_edit"
            - define text "Could not find Denizen Player Models."
            - define text_2 "<gray>Forums: <green><&click[https://forum.denizenscript.com/resources/denizen-player-models.107/].type[OPEN_URL]>click here<&end_click><gray>."
            - define text_3 "<gray>Github: <green><&click[https://github.com/FutureMaximus/Denizen-Player-Models].type[OPEN_URL]>click here<&end_click><gray>."
            - define text_4 "<gray>Wiki: <green><&click[https://github.com/FutureMaximus/Denizen-Player-Models/wiki].type[OPEN_URL]>click here<&end_click><gray>."
            - narrate "<[msg_prefix]> <gray><[text]>"
            - narrate <[text_2]>
            - narrate <[text_3]>
            - narrate <[text_4]>
            - inventory close
          - else:
            - choose <[arg]>:
              #-New player model preparation
              - case new:
                - run dcutscene_model_list_new def.tick:<[tick]> def.scene:<[data.name]> def.type:player_model

              #-Create new player model
              - case create:
                - define arg_3 <[arg_3]||null>
                #ID Set
                - if <[arg_2]> == id_set && <[arg_3]> != null:
                  #Check if model has already been set in tick
                  - define model_list <[data.keyframes.models.<[tick]>.model_list]||<list>>
                  - foreach <[model_list]> as:model_uuid:
                    - define model_id <[data.keyframes.models.<[tick]>.<[model_uuid]>.id]>
                    - if <[model_id]> == <[arg_3]>:
                      - define model_type <[data.keyframes.models.<[tick]>.<[model_uuid]>.type]>
                      - define text "There is already a <[model_type].replace[_].with[ ]> with an id of <green><[arg_3]> <gray>in tick <green><[tick]>t<gray>."
                      - narrate "<[msg_prefix]> <gray><[text]>"
                      - stop
                  #If there is a model present remove it
                  - if <player.has_flag[dcutscene_location_editor]>:
                    - define loc_data <player.flag[dcutscene_location_editor]>
                    - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                  - flag <player> cutscene_modify:new_player_model_location
                  #Save data for continuous data input in modifiers
                  - flag <player> dcutscene_save_data.id:<[arg_3]>
                  - define skin <proc[dcutscene_determine_player_model_skin].context[<[data.name]>|<[tick]>|<[uuid]>]>
                  - if <[skin]> == none || <[skin]> == player:
                    - define skin <player>
                  - run pmodels_spawn_model def:<player.location>|<[skin]>|<player> save:spawned
                  - define root <entry[spawned].created_queue.determination.first>
                  - flag <player> dcutscene_save_data.root:<[root]>
                  #Give the location tool
                  - run dcutscene_location_tool_give_data def:<player.location>|<[root]>|<[root].location.yaw>|player_model|<[skin]>
                  - define text "After choosing your location for this player model click <green>Confirm Location <gray>in the location GUI or chat <green>confirm<gray>. To re-open the location GUI do /dcutscene location."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - inventory open d:dcutscene_inventory_location_tool

                #Set location and create new player model
                - else if <[arg_2]> == location_set && <[arg_3]> != null:
                  - flag <player> cutscene_modify:!
                  - define model_uuid <util.random_uuid>
                  - definemap model_data id:<player.flag[dcutscene_save_data.id]> type:player_model root:none sub_frames:none
                  - definemap ray_trace_data direction:floor liquid:false passable:false
                  - definemap path_data interpolation:linear rotate_interp:true rotate_mul:1.0 move:true location:<[arg_3]> animation:false ray_trace:<[ray_trace_data]> skin:none tick:<[tick]>
                  - define model_data.path.<[tick]> <[path_data]>
                  - define data.keyframes.models.<[tick]>.<[model_uuid]> <[model_data]>
                  - define data.keyframes.models.<[tick]>.model_list:->:<[model_uuid]>
                  #=-Debugger
                  - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                    - ~run dcutscene_debugger def:player_model_create|<[data.keyframes.models]>
                    - stop
                  - flag server dcutscenes.<[data.name]>:<[data]>
                  - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                  - define text "Denizen Player Model has been created for tick <green><[tick]>t <gray>with an ID of <green><player.flag[dcutscene_save_data.id]> <gray>in scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  #Remove created player model
                  - if <player.has_flag[dcutscene_location_editor]>:
                    - define loc_data <player.flag[dcutscene_location_editor]>
                    - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                  - run dcutscene_location_tool_return_inv
                  - flag <player> dcutscene_location_editor:!
                  - inventory open d:dcutscene_inventory_sub_keyframe
                  - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - else:
                  - debug error "Something went wrong in dcutscene_model_keyframe_edit for player_model modifier"

              #=Create new keyframe point for presently created model
              - case create_present:
                - choose <[arg_2]>:
                  #-Prepare for new keyframe
                  - case new_keyframe_prepare:
                    #Data from items in the models GUI
                    - define root_save <[arg_3]||null>
                    - if <[root_save]> != null:
                      #Get the starting point model data
                      - define root_tick <[root_save.tick]>
                      - define root_uuid <[root_save.uuid]>
                      - define root_data <[data.keyframes.models.<[root_tick]>.<[root_uuid]>]||null>
                      - if <[root_data]> == null:
                        - debug error "Something went wrong in dcutscene_model_keyframe_edit could not find root model keyframe"
                      - else:
                        - flag <player> cutscene_modify:new_player_model_keyframe_point
                        #If there is a present player model remove it
                        - if <player.has_flag[dcutscene_location_editor]>:
                          - define loc_data <player.flag[dcutscene_location_editor]>
                          - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                        - define text "After choosing your location for this player model click <green>Confirm Location <gray>in the location GUI or chat <green>confirm<gray>. To re-open the location GUI do /dcutscene location."
                        - narrate "<[msg_prefix]> <gray><[text]>"
                        - flag <player> dcutscene_save_data.data:<[root_save]>
                        - define skin <[data.name].proc[dcutscene_determine_player_model_skin].context[<[root_tick]>|<[root_uuid]>]>
                        - if <[skin]> == none || <[skin]> == player:
                          - define skin <player>
                        - run pmodels_spawn_model def:<player.location>|<[skin].parsed>|<player> save:spawned
                        - define root <entry[spawned].created_queue.determination.first>
                        - flag <player> dcutscene_save_data.root:<[root]>
                        - run dcutscene_location_tool_give_data def:<player.location>|<[root]>|<[root].location.yaw>|player_model|<[skin]>
                        - inventory open d:dcutscene_inventory_location_tool

                  #-Set the new keyframe point
                  - case new_keyframe_set:
                    - flag <player> cutscene_modify:!
                    - define loc <location[<[arg_3]>]>
                    #Root Model Data
                    - define root_save <player.flag[dcutscene_save_data.data]||null>
                    - define root_tick <[root_save.tick]>
                    - define root_uuid <[root_save.uuid]>
                    - define root_id <[root_save.id]>
                    #Default data for new model
                    - definemap ray_trace_data direction:floor liquid:false passable:false
                    - definemap path_data rotate_interp:true rotate_mul:1.0 interpolation:linear location:<[loc]> move:true animation:false ray_trace:<[ray_trace_data]> skin:none tick:<[tick]>
                    #Update the root data
                    - define path <[data.keyframes.models.<[root_tick]>.<[root_uuid]>.path]>
                    - define path.<[tick]> <[path_data]>
                    #Update root model
                    - flag server dcutscenes.<[data.name]>.keyframes.models.<[root_tick]>.<[root_uuid]>.path:<[path]>
                    - flag server dcutscenes.<[data.name]>.keyframes.models.<[root_tick]>.<[root_uuid]>.sub_frames.<[tick]>:<[root_uuid]>
                    #Set new model
                    - define model_uuid <[root_uuid]>
                    - definemap model_data id:<[root_id]> type:player_model path:false
                    - define model_data.root.tick <[root_tick]>
                    - define model_data.root.uuid <[root_uuid]>
                    #=-Debugger
                    - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                      - ~run dcutscene_debugger def:player_model_new_keyframe_set|<[model_data]>
                      - stop
                    - flag server dcutscenes.<[data.name]>.keyframes.models.<[tick]>.<[model_uuid]>:<[model_data]>
                    - flag server dcutscenes.<[data.name]>.keyframes.models.<[tick]>.model_list:->:<[model_uuid]>
                    - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                    #Sort ticks by time
                    - run dcutscene_sort_data def.cutscene:<[data.name]>
                    #Remove created player model
                    - if <player.has_flag[dcutscene_location_editor]>:
                      - define loc_data <player.flag[dcutscene_location_editor]>
                      - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                    - run dcutscene_location_tool_return_inv
                    - flag <player> dcutscene_location_editor:!
                    - inventory open d:dcutscene_inventory_sub_keyframe
                    - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                    - define text "Player model <green><[model_data.id]> <gray>has been set to tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                    - narrate "<[msg_prefix]> <gray><[text]>"

              #-Change id of player model
              - case change_id:
                - choose <[arg_2]>:
                  #Prepare for new id
                  - case new_id_prepare:
                    - flag <player> cutscene_modify:set_player_model_id
                    - flag <player> dcutscene_save_data.type:player_model
                    - define text "Chat the new id of the player model."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - inventory close

                  #Set the player model id
                  - case id_set:
                    - flag <player> cutscene_modify:!
                    - define model_data <[data.keyframes.models]>
                    - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                    #If there is a root update the root and all sub frames
                    - if <[root_data]> != none:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.id <[arg_3]>
                      - define sub_frames <[model_data.<[root_data.tick]>.<[root_data.uuid]>.sub_frames]||none>
                      - if <[sub_frames]> != none:
                        - foreach <[sub_frames]> key:frame_tick as:frame_uuid:
                          - define model_data.<[frame_tick]>.<[frame_uuid]>.id <[arg_3]>
                    #If the model is a root data model update the sub frames should they exist
                    - else:
                      - define model_data.<[tick]>.<[uuid]>.id <[arg_3]>
                      - define sub_frames <[model_data.<[tick]>.<[uuid]>.sub_frames]||none>
                      - if <[sub_frames]> != none:
                        - foreach <[sub_frames]> key:frame_tick as:frame_uuid:
                          - define model_data.<[frame_tick]>.<[frame_uuid]>.id <[arg_3]>
                    #=-Debugger
                    - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                      - ~run dcutscene_debugger def:player_model_set_ID|<[model_data]>
                      - stop
                    - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                    - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                    - define text "Player model ID changed to <green><[arg_3]> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - inventory open d:dcutscene_inventory_sub_keyframe
                    - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

              #-New location for player model
              - case location:
                - choose <[arg_2]>:
                  #Preparation for a new player model location
                  - case new_location_prepare:
                    #If there is a present player model remove it
                    - if <player.has_flag[dcutscene_location_editor]>:
                      - define loc_data <player.flag[dcutscene_location_editor]>
                      - run dcutscene_model_remove def:<[loc_data.root_type]>|<[loc_data.root_ent]>
                    - define model_data  <[data.keyframes.models]>
                    - define root_save <[model_data.<[tick]>.<[uuid]>.root]||none>
                    - if <[root_save]> == none:
                      - define root_save.tick <[tick]>
                      - define root_save.uuid <[uuid]>
                    - define text "After choosing your location for this player model click <green>Confirm Location <gray>in the location GUI or chat <green>confirm<gray>. To re-open the location GUI do /dcutscene location."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - flag <player> cutscene_modify:set_new_player_model_location
                    - flag <player> dcutscene_save_data.data:<[root_save]>
                    - define skin <proc[dcutscene_determine_player_model_skin].context[<[data.name]>|<[tick]>|<[uuid]>]>
                    - if <[skin]> == none || <[skin]> == player:
                      - define skin <player>
                    - run pmodels_spawn_model def:<player.location>|<[skin].parsed>|<player> save:spawned
                    - define root <entry[spawned].created_queue.determination.first>
                    - flag <player> dcutscene_save_data.root:<[root]>
                    - run dcutscene_location_tool_give_data def:<player.location>|<[root]>|<[root].location.yaw>|player_model|<[skin]>
                    - inventory open d:dcutscene_inventory_location_tool

                  #Sets the new player model location
                  - case set_location:
                    - flag <player> cutscene_modify:!
                    - define model_data <[data.keyframes.models]>
                    - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                    - if <[root_data]> != none:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.location <[arg_3]>
                    - else:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.location <[arg_3]>
                    #=-Debugger
                    - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                      - ~run dcutscene_debugger def:player_model_set_location|<[model_data]>
                      - stop
                    - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                    - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                    - define text "Player Model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>location is now <green><[arg_3].simple> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - inventory open d:dcutscene_inventory_keyframe_modify_player_model
                    - define model_root <player.flag[dcutscene_save_data.root]||null>
                    - if <[model_root]> != null || <[model_root].is_spawned>:
                      - run pmodels_remove_model def:<[model_root]>
                    - run dcutscene_location_tool_return_inv
                    - flag <player> dcutscene_location_editor:!

              #-Animation for player model
              - case animate:
                - choose <[arg_2]>:
                  #Prepare for new animation
                  - case new_animation_prepare:
                    - flag <player> cutscene_modify:set_model_animation
                    - flag <player> dcutscene_save_data.type:player_model
                    - define text "To set the animation use the command /dcutscene animate <green>my_animation <gray>to prevent an animation from playing put <red>false<gray> to stop an animation from playing put <red>stop<gray>."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - inventory close

                  #Set the animation for the player model keyframe point
                  - case set_animation:
                    - define arg_3 <[arg_3]||null>
                    - if <[arg_3]> != null:
                      - flag <player> cutscene_modify:!
                      - define model_data <[data.keyframes.models]>
                      - define root_data <[data.keyframes.models.<[tick]>.<[uuid]>.root]||none>
                      #If the model contains a root data model
                      - if <[root_data]> != none:
                        - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.animation <[arg_3]>
                      #If the model is a root model
                      - else:
                        - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.animation <[arg_3]>
                      #=-Debugger
                      - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                        - ~run dcutscene_debugger def:player_model_set_animation|<[model_data]>
                        - stop
                      - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                      - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                      - if <[arg_3]> != stop:
                        - define text "Player Model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>animation is now <green><[arg_3]> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                      - else:
                        - define text "Player Model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>animation will now <red><[arg_3]> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                      - narrate "<[msg_prefix]> <gray><[text]>"
                      - inventory open d:dcutscene_inventory_keyframe_modify_player_model

              #-Whether the model will move to the next keyframe point
              - case set_move:
                - define model_data <[data.keyframes.models]>
                - define root_data <[data.keyframes.models.<[tick]>.<[uuid]>.root]||none>
                #If the model contains a root data model
                - if <[root_data]> != none:
                  - define move <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.move]>
                  - choose <[move]>:
                    - case true:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.move false
                      - define move false
                    - case false:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.move true
                      - define move true
                #If the model is a root model
                - else:
                  - define move <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.move]>
                  - choose <[move]>:
                    - case true:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.move false
                      - define move false
                    - case false:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.move true
                      - define move true
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:player_model_set_move|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define l1 "<gold>Move: <gray><[move]>"
                - define l2 "<gray><italic>Click to change player model move"
                - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

              #-Ray Trace Modifying
              - case ray_trace:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                #Ray Trace Data
                - if <[root_data]> != none:
                  - define tick_modify <[root_data.tick]>
                  - define uuid_modify <[root_data.uuid]>
                  - define ray_trace <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.ray_trace]>
                - else:
                  - define tick_modify <[tick]>
                  - define uuid_modify <[uuid]>
                  - define ray_trace <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.ray_trace]>
                - choose <[arg_2]>:
                  #Ray Trace Direction
                  - case ray_trace_direction:
                    - define direction <[ray_trace.direction]||floor>
                    - choose <[direction]>:
                      - case false:
                        - define direction floor
                      - case floor:
                        - define direction ceiling
                      - case ceiling:
                        - define direction false
                    - define l1 "<red>Direction: <gray><[direction]>"
                    - define l2 "<gray><italic>Click to change ray trace direction"
                    - define model_data.<[tick_modify]>.<[uuid_modify]>.path.<[tick]>.ray_trace.direction:<[direction]>
                  #Ray Trace Liquid
                  - case ray_trace_liquid:
                    - define liquid <[ray_trace.liquid]||false>
                    - choose <[liquid]>:
                      - case false:
                        - define liquid true
                      - case true:
                        - define liquid false
                    - define l1 "<blue>Liquid: <gray><[liquid]>"
                    - define l2 "<gray><italic>Click to change ray trace liquid"
                    - define model_data.<[tick_modify]>.<[uuid_modify]>.path.<[tick]>.ray_trace.liquid:<[liquid]>
                  #Ray Trace Passable
                  - case ray_trace_passable:
                    - define passable <[ray_trace.passable]||false>
                    - choose <[passable]>:
                      - case false:
                        - define passable true
                      - case true:
                        - define passable false
                    - define l1 "<green>Passable: <gray><[passable]>"
                    - define l2 "<gray><italic>Click to change ray trace passable"
                    - define model_data.<[tick_modify]>.<[uuid_modify]>.path.<[tick]>.ray_trace.passable:<[passable]>
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:player_model_ray_trace|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_3]> lore:<[lore]>

              #-Set the path interpolation method
              - case change_path_interp:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                #If the model contains a root data model
                - if <[root_data]> != none:
                  - define interp <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.interpolation]>
                  - choose <[interp]>:
                    - case linear:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.interpolation smooth
                      - define interp smooth
                    - case smooth:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.interpolation linear
                      - define interp linear
                #If the model is a root data model
                - else:
                  - define interp <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.interpolation]>
                  - choose <[interp]>:
                    - case linear:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.interpolation smooth
                      - define interp smooth
                    - case smooth:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.interpolation linear
                      - define interp linear
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:player_model_path_interp|<[model_data]>
                  - stop
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define l1 "<blue>Interpolation: <gray><[interp]>"
                - define l2 "<gray><italic>Click to change path interpolation method"
                - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

              #-Change player model skin
              - case change_skin:
                - choose <[arg_2]>:
                  #Preparation for new skin
                  - case new_skin_prepare:
                    - define text "Chat a valid tag of a player or npc. To use the player's skin chat <green>player <gray>to do nothing chat <green>none<gray>. Chat <red>cancel <gray>to stop."
                    - narrate "<[msg_prefix]> <gray><[text]>"
                    - flag <player> cutscene_modify:player_model_change_skin
                    - inventory close
                  #Set the new skin
                  - case set_new_skin:
                    - define arg_3 <[arg_3]||null>
                    - if <[arg_3]> != null:
                      - define model_data <[data.keyframes.models]>
                      - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                      - if <[root_data]> != none:
                        - define tick_modify <[root_data.tick]>
                        - define uuid_modify <[root_data.uuid]>
                      - else:
                        - define tick_modify <[tick]>
                        - define uuid_modify <[uuid]>
                      - define model_data.<[tick_modify]>.<[uuid_modify]>.path.<[tick]>.skin:<[arg_3]>
                      #=-Debugger
                      - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                        - ~run dcutscene_debugger def:player_model_change_skin|<[model_data]>
                        - stop
                      - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                      - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                      - inventory open d:dcutscene_inventory_keyframe_modify_player_model
                      - define text "Player model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>skin is now <green><[arg_3].parsed||<[arg_3]>> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                      - narrate "<[msg_prefix]> <gray><[text]>"

              #-Change rotation interpolate
              - case change_rotate_interp:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                #If the model contains a root data model
                - if <[root_data]> != none:
                  - define interp <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.rotate_interp]||false>
                  - choose <[interp]>:
                    - case true:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.rotate_interp false
                      - define interp false
                    - case false:
                      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.rotate_interp true
                      - define interp true
                #If the model is a root data model
                - else:
                  - define interp <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.rotate_interp]||false>
                  - choose <[interp]>:
                    - case true:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.rotate_interp false
                      - define interp false
                    - case false:
                      - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.rotate_interp true
                      - define interp true
                #=-Debugger
                - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                  - ~run dcutscene_debugger def:player_model_change_rotate_interp|<[model_data]>
                  - stop
                #Update data
                - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define l1 "<dark_purple>Rotate interpolation: <gray><[interp]>"
                - define l2 "<gray><italic>Click to change path rotation interpolation"
                - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
                - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

              #-Change rotate mul prep
              - case change_rotate_mul_prep:
                - define text "Chat a valid number for the rotation multiplier."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:player_model_change_rotate_mul expire:2m
                - inventory close

              #-Change rotate multiplier
              - case change_rotate_mul:
                - if <[arg_2].is_decimal>:
                  - flag <player> cutscene_modify:!
                  - define model_data <[data.keyframes.models]>
                  - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                  #If the model contains a root data model
                  - if <[root_data]> != none:
                    - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.rotate_mul <[arg_2]>
                  #If the model is a root data model
                  - else:
                    - define model_data.<[tick]>.<[uuid]>.path.<[tick]>.rotate_mul <[arg_2]>
                  #=-Debugger
                  - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
                    - ~run dcutscene_debugger def:player_model_rotate_mul|<[model_data]>
                    - stop
                  #Update data
                  - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
                  - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                  - define text "Player model <green><[model_data.<[tick]>.<[uuid]>.id]> <gray>rotation multiplier is now <green><[arg_2]> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - inventory open d:dcutscene_inventory_keyframe_modify_player_model
                - else:
                  - define text "<green><[arg_2]> <gray>is not a valid number."
                  - narrate "<[msg_prefix]> <gray><[text]>"

              #-Teleport to player model location
              - case teleport_to:
                - define model_data <[data.keyframes.models]>
                - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
                - if <[root_data]> != none:
                  - define tick_mod <[root_data.tick]>
                  - define tick_uuid <[root_data.uuid]>
                - else:
                  - define tick_mod <[tick]>
                  - define tick_uuid <[uuid]>
                - define loc <[model_data.<[tick_mod]>.<[tick_uuid]>.path.<[tick]>.location]||null>
                - if <[loc]> == null:
                  - debug error "Could not find location to teleport to in dcutscene_model_keyframe_edit for player model"
                - else:
                  - teleport <player> <location[<[loc]>].with_pitch[<player.location.pitch>]>
                  - define text "You have teleported to player model <green><[model_data.<[tick_mod]>.<[tick_uuid]>.id]> <gray>location in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - inventory open d:dcutscene_inventory_keyframe_modify_player_model

              #-Move to prep
              - case move_to_prep:
                - define player_model_data <[data.keyframes.models.<[tick]>.<[uuid]>]>
                - definemap move_data animator:player_model type:move tick:<[tick]> uuid:<[uuid]> data:<[player_model_data]> scene:<[data.name]>
                - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
                - define text "Click on the tick you'd like to move this player model animator to. To stop chat <red>cancel<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - ~run dcutscene_keyframe_modify def:back

              #-Move to
              - case move_to:
                - define move_data <player.flag[dcutscene_animator_change]>
                - ~run dcutscene_move_model_animator save:result
                - if <entry[result].created_queue.determination.first> == valid:
                  - define text "Animator <green><[move_data.animator].replace[_].with[ ]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"

              #-Duplicate prep
              - case duplicate_prep:
                - define model_data <[data.keyframes.models.<[tick]>.<[uuid]>]>
                - definemap dup_data animator:model type:duplicate tick:<[tick]> uuid:<[uuid]> data:<[model_data]> scene:<[data.name]>
                - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
                - define text "Click on the tick you'd like to duplicate this model animator to. To stop chat <red>cancel<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - ~run dcutscene_keyframe_modify def:back

              #-Duplicate
              - case duplicate:
                - define dup_data <player.flag[dcutscene_animator_change]>
                - ~run dcutscene_duplicate_model_animator save:result
                - if <entry[result].created_queue.determination.first> == valid:
                  - define text "Animator <green><[dup_data.animator].replace[_].with[ ]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"

              #-Play the cutscene from this tick
              - case play_from_here:
                - inventory close
                - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
                - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

              #-Removes model from tick
              - case remove_tick:
                - run dcutscene_remove_model_animator_from_tick def.type:player_model

              #-Removes model from entire cutscene
              - case remove_all:
                - run dcutscene_remove_model_animator_all def.type:player_model

#= Multi operation tasks for model animators

# If there are previously made models they are displayed for modification to add to a new tick if not create a new model instead
dcutscene_model_list_new:
    type: task
    debug: false
    definitions: tick|scene|type|page
    script:
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - define keyframes <server.flag[dcutscenes.<[scene]>.keyframes.models]||<map>>
    - define max 9
    - define prev_models <list>
    #Look for previously made models
    - foreach <[keyframes]> key:time as:model:
      - if <[time]> < <[tick]>:
        #Exclude that models that are not the type specified
        - foreach <[model.model_list]> as:model_uuid:
          - define model_data <[model.<[model_uuid]>]>
          - if <[model_data.type]> != <[type]>:
            - define model <[model].deep_exclude[<[model_uuid]>]>
        - if <[model].deep_exclude[model_list].any>:
          #Set the previously created models as data to be utilized
          - definemap model_prev tick:<[time]> data:<[model]>
          - define prev_models:->:<[model_prev]>
    #If there are no previously made models create a new model animator immediately
    - if <[prev_models].is_empty>:
      - choose <[type]>:
        - case model:
          - flag <player> cutscene_modify:new_model_id expire:3m
        - case player_model:
          - flag <player> cutscene_modify:new_player_model_id expire:3m
      - define text "Chat the name of the <[type].replace[_].with[ ]> this will be used as an identifier."
      - narrate "<[msg_prefix]> <gray><[text]>"
      - inventory close
    #If there are previously created models set them as data items in a GUI
    - else:
      - choose <[type]>:
        - case model:
          - define item <item[dragon_head]>
        - case player_model:
          - define item <item[player_head]>
      - flag <player> dcutscene_save_data.type:<[type]>
      - flag <player> dcutscene_save_data.scene_name:<[scene]>
      - inventory open d:dcutscene_inventory_keyframe_model_list
      - define inv <player.open_inventory>
      #Page index
      - if !<player.has_flag[dcutscene_model_list_page_index]>:
        - flag <player> dcutscene_model_list_page_index:1
        - define page_index 1
      - else:
        - define page_index <player.flag[dcutscene_model_list_page_index]>
      - if <[page]||null> == null:
        - define page_index:1
        - flag <player> dcutscene_model_list_page_index:1
      - else:
        - choose <[page]>:
          - case next:
            - flag <player> dcutscene_model_list_page_index:++
            - define page_index <player.flag[dcutscene_model_list_page_index]>
          - case previous:
            - flag <player> dcutscene_model_list_page_index:<[page_index].sub[1].equals[0].if_true[1].if_false[<[page_index].sub[1]>]>
            - define page_index <player.flag[dcutscene_model_list_page_index]>
      - define max 45
      - define limit <[max].mul[<[page_index]>]>
      - define start <[limit].sub[<[max].sub[1]>]>
      - define new_model_list <list>
      - define exceed false
      - define less false
      - foreach <[prev_models]> as:model:
        - foreach <[model.data.model_list]||<list>> as:model_uuid:
          - define size:++
          - if <[size]> < <[start]>:
            - define less true
            - foreach next
          - if <[size]> > <[limit]>:
            - define exceed true
            - foreach next
          - define model_data <[model.data.<[model_uuid]>]||null>
          - if <[model_data]> == null:
            - foreach next
          #Model item set
          - define model_type <[model_data.type]>
          - if <[model_type]> == model && <[model_data.root]||none> == none && <[model_type]> == <[type]>:
            - define slot:++
            - definemap item_data type:<[model_data.type]> tick:<[model.tick]> uuid:<[model_uuid]> id:<[model_data.id]>
            - define display <blue><bold><[model_data.id]>
            - define l1 "<blue>Type: <gray>Model"
            - define l2 "<blue>Starting Time <gray><[model.tick]>t"
            - define l3 "<gray><italic>Click to modify this model"
            - adjust <[item]> display:<[display]> save:item
            - define item <entry[item].result>
            - adjust <[item]> lore:<list[<empty>|<[l1]>|<[l2]>|<empty>|<[l3]>]> save:item
            - define item <entry[item].result>
            #Input the data so the creator can modify this
            - flag <[item]> model_keyframe_modify:<[item_data]>
            - inventory set d:<[inv]> o:<[item]> slot:<[slot]>
          #Player model item set
          - else if <[model_type]> == player_model && <[model_data.root]||none> == none && <[model_type]> == <[type]>:
            - define slot:++
            - definemap item_data type:<[model_data.type]> tick:<[model.tick]> uuid:<[model_uuid]> id:<[model_data.id]>
            - define skin <[model_data.path.<[model.tick]>.skin].parsed||none>
            - if <[skin]> == none || <[skin]> == player:
              - define skin <player>
            - adjust <[item]> skull_skin:<[skin].skull_skin> save:item
            - define item <entry[item].result>
            - define display <blue><bold><[model_data.id]>
            - define l1 "<blue>Type: <gray>Player Model"
            - define l2 "<blue>Starting Time <gray><[model.tick]>t"
            - define l3 "<gray><italic>Click to modify this player model"
            - adjust <[item]> display:<[display]> save:item
            - define item <entry[item].result>
            - adjust <[item]> lore:<list[<empty>|<[l1]>|<[l2]>|<empty>|<[l3]>]> save:item
            - define item <entry[item].result>
            #Input the data so the creator can modify this
            - flag <[item]> model_keyframe_modify:<[item_data]>
            - inventory set d:<[inv]> o:<[item]> slot:<[slot]>
      - if <[exceed]> && <[page_index]> != 1:
        - inventory set d:<player.open_inventory> slot:52 o:dcutscene_next
        - inventory set d:<player.open_inventory> slot:48 o:dcutscene_previous
      - else if <[exceed]>:
        - inventory set d:<player.open_inventory> slot:52 o:dcutscene_next
      - else if <[less]>:
        - inventory set d:<player.open_inventory> slot:48 o:dcutscene_previous

# Move a model animator to a new tick
dcutscene_move_model_animator:
    type: task
    debug: false
    script:
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - define data <player.flag[cutscene_data]>
    - define tick <player.flag[dcutscene_tick_modify]>
    - define move_data <player.flag[dcutscene_animator_change]>
    - define model_data <[data.keyframes.models]>
    - define root_data <[model_data.<[move_data.tick]>.<[move_data.uuid]>.root]||none>
    #Condition Checks
    #= Non-root model (sub-frame)
    - if <[root_data]> != none:
      - define root_tick <[root_data.tick]>
      #If the tick is less than the root tick stop and tell the user
      - if <[tick]> <= <[root_tick]>:
        - define text "The tick must be greater than the root data tick <green><[root_tick]>t <gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - determine passively invalid
        - stop
      #-Root Update
      #Data for new tick in path
      - define path_tick <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[move_data.tick]>].deep_with[tick].as[<[tick]>]>
      #Remove old ticks in path for root data
      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path].deep_exclude[<[move_data.tick]>]>
      #Update the path for root model
      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>:<[path_tick]>
      #Update sub frames
      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.sub_frames <[model_data.<[root_data.tick]>.<[root_data.uuid]>.sub_frames].deep_exclude[<[move_data.tick]>]>
      - define model_data.<[root_data.tick]>.<[root_data.uuid]>.sub_frames.<[tick]>:<[move_data.uuid]>
      #-Model Update
      #Remove (non-root) model from tick
      - define model_data.<[move_data.tick]> <[model_data.<[move_data.tick]>].deep_exclude[<[move_data.uuid]>]>
      #Remove from the model list
      - define model_data.<[move_data.tick]>.model_list:<-:<[move_data.uuid]>
      - if <[model_data.<[move_data.tick]>.model_list].is_empty>:
        - define model_data <[model_data].deep_exclude[<[move_data.tick]>]>
      #Set new data
      - define model_data.<[tick]>.<[move_data.uuid]>:<[move_data.data]>
      - define model_data.<[tick]>.model_list:->:<[move_data.uuid]>
      - determine passively valid
    #= Root Model
    - else:
      #If root model is already at tick stop
      - if <[move_data.tick]> == <[tick]>:
        - define text "There is already a root data tick there."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - determine passively invalid
        - stop
      - define path <[model_data.<[move_data.tick]>.<[move_data.uuid]>.path]||<map>>
      #If the model is a root model ensure it does not exceed the path ticks if available
      - if <[path].any>:
        - define path_keys <[path].keys||<list>>
        - foreach <[path_keys]> as:key:
          - if <[key]> > <[move_data.tick]> && <[key]> != <[move_data.tick]>:
            - define first <[key]>
            - foreach stop
        - if <[first]||null> != null:
          - if <[tick]> >= <[first]>:
            - define text "A root data tick cannot exceed sub frames."
            - narrate "<[msg_prefix]> <gray><[text]>"
            - determine passively invalid
            - stop
      #Update Sub Frames
      - foreach <[model_data.<[move_data.tick]>.<[move_data.uuid]>.sub_frames]> key:sub_tick as:sub_uuid:
        - define model_data.<[sub_tick]>.<[sub_uuid]>.root.tick:<[tick]>
      #Update Root Data
      - define model_data.<[move_data.tick]> <[model_data.<[move_data.tick]>].deep_exclude[<[move_data.uuid]>]>
      - define model_data.<[move_data.tick]>.model_list:<-:<[move_data.uuid]>
      - if <[model_data.<[move_data.tick]>.model_list].is_empty>:
        - define model_data <[model_data].deep_exclude[<[move_data.tick]>]>
      #Set new data
      - define model_data.<[tick]>.<[move_data.uuid]>:<[move_data.data]>
      - define model_data.<[tick]>.model_list:->:<[move_data.uuid]>
      #Data for new tick in path
      - define path_tick <[model_data.<[tick]>.<[move_data.uuid]>.path.<[move_data.tick]>].deep_with[tick].as[<[tick]>]>
      - define model_data.<[tick]>.<[move_data.uuid]>.path.<[tick]>:<[path_tick]>
      #Remove old ticks in path for root data
      - define model_data.<[tick]>.<[move_data.uuid]>.path <[model_data.<[tick]>.<[move_data.uuid]>.path].deep_exclude[<[move_data.tick]>]>
      - determine passively valid
    #=-Debugger
    - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
      - ~run dcutscene_debugger def:model_move_to|<[model_data]>
      - stop
    #Set New Data
    - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
    - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
    - flag <player> dcutscene_animator_change:!
    - ~run dcutscene_sort_data def.cutscene:<[data.name]>
    - inventory open d:dcutscene_inventory_sub_keyframe
    - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

# Duplicate a model animator to a new tick
dcutscene_duplicate_model_animator:
    type: task
    debug: false
    script:
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - define data <player.flag[cutscene_data]>
    - define tick <player.flag[dcutscene_tick_modify]>
    - define dup_data <player.flag[dcutscene_animator_change]>
    - define dup_tick <[dup_data.tick]>
    - define dup_uuid <[dup_data.uuid]>
    - define model_data <[data.keyframes.models]>
    - define root_data <[model_data.<[dup_data.tick]>.<[dup_data.uuid]>.root]||none>
    # Validation and data setting
    #= Non-root model (sub-frame)
    - if <[root_data]> != none:
      - define root_tick <[root_data.tick]>
      - define root_uuid <[root_data.uuid]>
      #If the tick is less than the root tick stop and tell the creator
      - if <[tick]> <= <[root_tick]>:
        - define text "The tick must be greater than the root data tick <green><[root_tick]>t <gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - determine passively invalid
        - stop
      #Check if there is already a sub frame tick
      - define path <[model_data.<[root_tick]>.<[root_uuid]>.path]>
      - define path_keys <[path].keys||<list>>
      - foreach <[path_keys]> as:key:
        - if <[key]> == <[tick]>:
          - define text "There is already a sub frame at that tick."
          - narrate "<[msg_prefix]> <gray><[text]>"
          - determine passively invalid
          - stop
      #-Root Update
      #Memory data for new tick in path
      - define path_tick <[path.<[dup_data.tick]>].deep_with[tick].as[<[tick]>]>
      #Update the path for root model
      - define model_data.<[root_tick]>.<[root_uuid]>.path.<[tick]>:<[path_tick]>
      #Update sub frames
      - define model_data.<[root_tick]>.<[root_uuid]>.sub_frames.<[tick]>:<[dup_data.uuid]>
      #-Model Update
      #Set new data
      - define model_data.<[tick]>.<[dup_data.uuid]>:<[dup_data.data]>
      - define model_data.<[tick]>.model_list:->:<[dup_data.uuid]>
      - determine passively valid
    #= Root Model
    - else:
      #If root model is already at tick stop
      - if <[dup_data.tick]> == <[tick]>:
        - define text "There is already a root data tick there."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - determine passively invalid
        - stop
      #If the model used to be a root model ensure it does not go below the previous root model
      - else if <[tick]> < <[dup_data.tick]>:
        - define text "The tick must be greater than the previous root data tick <green><[dup_data.tick]>t<gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - determine passively invalid
        - stop
      #Check if there is already a sub frame at the tick
      - else:
        - define path <[model_data.<[dup_data.tick]>.<[dup_data.uuid]>.path]||<map>>
        - define path_keys <[path].keys||<list>>
        - foreach <[path_keys]> as:key:
          - if <[key]> == <[tick]>:
            - define text "There is already a sub frame at that tick."
            - narrate "<[msg_prefix]> <gray><[text]>"
            - determine passively invalid
            - stop
      - define new_root_data <[model_data.<[dup_data.tick]>.<[dup_data.uuid]>].deep_exclude[sub_frames]>
      - define new_root_data.path false
      - definemap root_map tick:<[dup_data.tick]> uuid:<[dup_data.uuid]>
      - define new_root_data.root:<[root_map]>
      - define model_data.<[tick]>.<[dup_data.uuid]>:<[new_root_data]>
      - define model_data.<[tick]>.model_list:->:<[dup_data.uuid]>
      #Update Root Model
      - define new_path_data <[model_data.<[dup_data.tick]>.<[dup_data.uuid]>.path.<[dup_data.tick]>].with[tick].as[<[tick]>]>
      #Root Path Update
      - define model_data.<[dup_data.tick]>.<[dup_data.uuid]>.path.<[tick]>:<[new_path_data]>
      #Root sub-frames Update
      - define model_data.<[dup_data.tick]>.<[dup_data.uuid]>.sub_frames.<[tick]>:<[dup_data.uuid]>
      - determine passively valid
    #=-Debugger
    - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
      - ~run dcutscene_debugger def:model_duplicate|<[model_data]>
      - stop
    #Set New Data
    - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
    - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
    - flag <player> dcutscene_animator_change:!
    - ~run dcutscene_sort_data def.cutscene:<[data.name]>
    - inventory open d:dcutscene_inventory_sub_keyframe
    - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

# Remove model animator from tick
dcutscene_remove_model_animator_from_tick:
    type: task
    debug: false
    definitions: type
    script:
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - define data <player.flag[cutscene_data]>
    - define tick_data <player.flag[dcutscene_tick_modify]>
    - define tick <[tick_data.tick]>
    - define uuid <[tick_data.uuid]>
    - define model_data <[data.keyframes.models]>
    - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
    #If the model has a root data model
    - if <[root_data]> != none:
      #Update the root data model
      - define root_update <[model_data.<[root_data.tick]>.<[root_data.uuid]>]>
      - define root_update.sub_frames <[root_update.sub_frames].deep_exclude[<[tick]>]>
      - define root_update.path <[root_update.path].deep_exclude[<[tick]>]>
      #Check if the updated data has empty sub_frames
      - if <[root_update.sub_frames].is_empty>:
        - define root_update.sub_frames none
      #Remove the specified tick player model
      - define model_data.<[root_data.tick]>.<[root_data.uuid]> <[root_update]>
      - define model_data.<[tick]>.model_list:<-:<[uuid]>
      - define model_data.<[tick]> <[model_data.<[tick]>].exclude[<[uuid]>]>
      #If the model list is empty remove tick
      - if <[model_data.<[tick]>.model_list].is_empty>:
        - define model_data <[model_data].deep_exclude[<[tick]>]>
      #=-Debugger
      - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
        - ~run dcutscene_debugger def:model_remove_tick|<[model_data]>
        - stop
      - choose <[type]>:
        - case model:
          - define text "Model <green><[data.keyframes.models.<[tick]>.<[uuid]>.id]> <gray>has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
        - case player_model:
          - define text "Player model <green><[data.keyframes.models.<[tick]>.<[uuid]>.id]> <gray>has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
      - narrate "<[msg_prefix]> <gray><[text]>"
      - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
      - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
      - inventory open d:dcutscene_inventory_sub_keyframe
      - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
    #If the model is a root data model
    - else:
      - inventory close
      - choose <[type]>:
        - case model:
          - clickable dcutscene_model_keyframe_edit def:denizen_model|remove_all usages:1 save:remove_model
          - define text "This is a starting point model removing this will remove the model from the cutscene proceed? <green><bold><element[Yes].on_hover[<[msg_prefix]> <gray>This will permanently remove this model from this scene.].type[SHOW_TEXT].on_click[<entry[remove_model].command>]>"
        - case player_model:
          - clickable dcutscene_model_keyframe_edit def:player_model|remove_all usages:1 save:remove_model
          - define text "This is a starting point player model removing this will remove the player model from the cutscene proceed? <green><bold><element[Yes].on_hover[<[msg_prefix]> <gray>This will permanently remove this player model from this scene.].type[SHOW_TEXT].on_click[<entry[remove_model].command>]>"
      - narrate "<[msg_prefix]> <gray><[text]>"

# Used to remove a model animator from the entire cutscene or the server flag dcutscenes
dcutscene_remove_model_animator_all:
    type: task
    debug: false
    definitions: type
    script:
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - define data <player.flag[cutscene_data]>
    - define tick_data <player.flag[dcutscene_tick_modify]>
    - define tick <[tick_data.tick]>
    - define uuid <[tick_data.uuid]>
    - define model_data <[data.keyframes.models]>
    - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
    #=Non root (sub-frame) model
    - if <[root_data]> != none:
      - define sub_frames <[model_data.<[root_data.tick]>.<[root_data.uuid]>.sub_frames]||<list>>
      #Remove sub frames
      - foreach <[sub_frames]> key:tick_id as:subframe:
        - define model_data.<[tick_id]> <[model_data.<[tick_id]>].deep_exclude[<[subframe]>]>
        - define model_data.<[tick_id]>.model_list:<-:<[subframe]>
        #If the model list is empty remove the tick
        - if <[model_data.<[tick_id]>.model_list].is_empty>:
          - define model_data <[model_data].deep_exclude[<[tick_id]>]>
      #Remove the root
      - define model_data.<[root_data.tick]> <[model_data.<[root_data.tick]>].exclude[<[root_data.uuid]>]>
      - define model_data.<[root_data.tick]>.model_list:<-:<[root_data.uuid]>
      #If the model list is empty remove the tick
      - if <[model_data.<[root_data.tick]>.model_list].is_empty>:
        - define model_data <[model_data].deep_exclude[<[root_data.tick]>]>
    #=Root Model
    - else:
      #Remove sub frames
      - define sub_frames <[model_data.<[tick]>.<[uuid]>.sub_frames]||<list>>
      - foreach <[sub_frames]> key:tick_id as:subframe:
        - define model_data.<[tick_id]> <[model_data.<[tick_id]>].deep_exclude[<[subframe]>]>
        - define model_data.<[tick_id]>.model_list:<-:<[subframe]>
        #If the model list is empty remove the tick
        - if <[model_data.<[tick_id]>.model_list].is_empty>:
          - define model_data <[model_data].deep_exclude[<[tick_id]>]>
      #Remove the root
      - define model_data.<[tick]> <[model_data.<[tick]>].deep_exclude[<[uuid]>]>
      - define model_data.<[tick]>.model_list:<-:<[uuid]>
      #If the model list is empty remove the tick
      - if <[model_data.<[tick]>.model_list].is_empty>:
        - define model_data <[model_data].deep_exclude[<[tick]>]>
    #=-Debugger
    - if <script[dcutscenes_config].data_key[config].get[cutscene_tool_debugger_mode].if_null[false].is_truthy>:
      - ~run dcutscene_debugger def:model_remove_all|<[model_data]>
      - stop
    - choose <[type]>:
      - case model:
        - define text "Model <green><[data.keyframes.models.<[tick]>.<[uuid]>.id]> <gray>has been removed from scene <green><[data.name]><gray>."
      - case player_model:
        - define text "Player model <green><[data.keyframes.models.<[tick]>.<[uuid]>.id]> <gray>has been removed from scene <green><[data.name]><gray>."
    - narrate "<[msg_prefix]> <gray><[text]>"
    - flag server dcutscenes.<[data.name]>.keyframes.models:<[model_data]>
    - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
    - inventory open d:dcutscene_inventory_sub_keyframe
    - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

# Used to determine the previous skin in the player model keyframe
dcutscene_determine_player_model_skin:
    type: procedure
    debug: false
    definitions: scene|tick|uuid
    script:
    - define data <server.flag[dcutscenes.<[scene]>]||null>
    - if <[data]> != null:
      - define model_data <[data.keyframes.models]>
      - define root_data <[model_data.<[tick]>.<[uuid]>.root]||none>
      - if <[root_data]> != none:
        - define skin_check <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path.<[tick]>.skin]||none>
        - if <[skin_check]> != none:
          - determine <[skin_check]>
        - define path <[model_data.<[root_data.tick]>.<[root_data.uuid]>.path]||<list>>
      - else:
        - define skin_check <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.skin]||none>
        - if <[skin_check]> != none:
          - determine <[skin_check]>
        - define path <[model_data.<[tick]>.<[uuid]>.path]||<list>>
      - foreach <[path]> key:tick_id as:path:
        - define compare <[tick_id].is_less_than[<[tick]>]>
        - if <[compare].is_truthy>:
          - define skin <[path.skin]||none>
          - if <[skin]> != none:
            - define list:->:<[path]>
      - define list <[list]||null>
      - if <[list]> == null:
        - determine <[model_data.<[tick]>.<[uuid]>.path.<[tick]>.skin]||none>
      - else:
        - determine <[list].last.get[skin]||none>
    - else:
      - debug error "Could not find scene in dcutscene_determine_player_model_skin"
      - determine none

#========== Regular Animators Modifiers ============
#List of regular animators (Type List means there can be multiple of the same animator in 1 tick Type Once means there can only be 1 per tick)
#- Sound TYPE: List
#- Particle TYPE: List
#- Title TYPE: Once
#- Cinematic Screeneffect TYPE: Once
#- Fake block or schematic TYPE: List
#- Send command to player or console TYPE: List
#- Run a custom task TYPE: List
#- Set the time for the player TYPE: Once
#- Set the weather for the player TYPE: Once

#Modify regular animators in cutscenes (Regular animators are things that play only once and do not use the path system such as the camera)
#Note that when creating a new animator it cannot use the keyframes definition instead
dcutscene_animator_keyframe_edit:
    type: task
    debug: false
    definitions: option|arg|arg_2|arg_3
    script:
    - define option <[option]||null>
    - define arg <[arg]||null>
    - if <[option]> == null && <[arg]> == null:
      - debug error "Something went wrong in dcutscene_animator_edit could not determine option"
    - else:
      - define data <player.flag[cutscene_data]>
      - define keyframes <[data.keyframes.elements]||null>
      - define tick <player.flag[dcutscene_tick_modify.tick].if_null[<player.flag[dcutscene_tick_modify]>]>
      - define uuid <player.flag[dcutscene_tick_modify.uuid]||>
      - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
      - choose <[option]>:
        #======== Run Task Modifier ========
        - case run_task:
          - choose <[arg]>:
            #Prepare for new run task
            - case new:
              - flag <player> cutscene_modify:run_task expire:2m
              - define text "Chat the name of the run task script."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory close

            #Create new run task
            - case create:
              - define script_check <script[<[arg_2]>]||null>
              - if <[script_check]> != null:
                - flag <player> cutscene_modify:!
                - define task_uuid <util.random_uuid>
                #Starter data creation
                - definemap run_task_data script:<[arg_2]> defs:false waitable:false delay:<duration[0s]>
                #List of run tasks
                - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task.<[tick]>.run_task_list:->:<[task_uuid]>
                #Set the starter data
                - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task.<[tick]>.<[task_uuid]>:<[run_task_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Run task <green><[arg_2]> <gray>has been created for tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "Could not find script named <green><[arg_2]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Prepare for new run task
            - case change_task_prepare:
              - flag <player> cutscene_modify:run_task_change expire:2m
              - define text "Chat the name of the run task script."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory close

            #Change the task
            - case change_task:
              - define script_check <script[<[arg_2]>]||null>
              - if <[script_check]> != null:
                - flag <player> cutscene_modify:!
                - define run_task <[keyframes.run_task.<[tick]>.<[uuid]>]>
                - if <[run_task]> != null:
                  - define run_task.script <[arg_2]>
                  - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task.<[tick]>.<[uuid]>:<[run_task]>
                  - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                  - inventory open d:dcutscene_inventory_keyframe_modify_run_task
                  - define text "Run task script has been changed to <green><[arg_2]> <gray>for tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "Could not find script named <green><[arg_2]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Prepare to set new definitions for run task
            - case task_definition:
              - flag <player> cutscene_modify:run_task_def_set expire:2.5m
              - define text "Chat the definition for this run task the input can be any valid tag."
              - define text_2 "Chat <red>false <gray>to disable definitions."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - narrate <gray><[text_2]>
              - inventory close

            #Set the definitions for the run task
            - case set_task_definition:
              - flag <player> cutscene_modify:!
              - define run_task <[keyframes.run_task.<[tick]>.<[uuid]>]||null>
              - if <[run_task]> != null:
                - define run_task.defs <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task.<[tick]>.<[uuid]>:<[run_task]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_keyframe_modify_run_task
                - define text "Run task <green><[run_task.script]> <gray>definition is set to <green><[arg_2]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change run task waitable boolean
            - case change_waitable:
              - define run_task <[keyframes.run_task.<[tick]>.<[uuid]>]||null>
              - if <[run_task]> != null:
                - choose <[run_task.waitable]||false>:
                  - case true:
                    - define run_task.waitable false
                  - case false:
                    - define run_task.waitable true
                - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task.<[tick]>.<[uuid]>:<[run_task]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define item <item[<[arg_2]>]>
                - define inv <player.open_inventory>
                - define lore "<gold><bold>Waitable <gray><[run_task.waitable]>"
                - define click "<gray><italic>Click to change waitable"
                - adjust <[item]> lore:<list[<empty>|<[lore]>|<empty>|<[click]>]> save:item
                - define item <entry[item].result>
                - inventory set d:<[inv]> o:<[arg_2]> slot:<[arg_3]>

            #Prepare for new run task delay
            - case delay_prepare:
              - flag <player> cutscene_modify:run_task_delay expire:2m
              - define text "Chat a duration for the delay in the run task. Example: 1s or 20t"
              - define text_2 "To disable this chat <green>0<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - narrate <gray><[text_2]>
              - inventory close

            #Change delay
            - case change_delay:
              - define arg_2 <duration[<[arg_2]>]||null>
              - if <[arg_2]> != null:
                - flag <player> cutscene_modify:!
                - define duration <[arg_2]>
                - define run_task <[keyframes.run_task.<[tick]>.<[uuid]>]>
                - define run_task.delay <[duration]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task.<[tick]>.<[uuid]>:<[run_task]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_keyframe_modify_run_task
                - define text "Delay for run task <green><[run_task.script]> <gray>has been set to <green><[arg_2]> <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid duration."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Move to new keyframe prepare
            - case move_to_prep:
              - define run_task_data <[keyframes.run_task.<[tick]>.<[uuid]>]>
              - definemap move_data animator:run_task type:move tick:<[tick]> uuid:<[uuid]> data:<[run_task_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this run task animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to new keyframe
            - case move_to:
              - define move_data <player.flag[dcutscene_animator_change]>
              - define run_task <[keyframes.run_task]>
              #Update previous tick
              - define run_task.<[move_data.tick]>.run_task_list:<-:<[move_data.uuid]>
              - define run_task.<[move_data.tick]> <[run_task.<[move_data.tick]>].deep_exclude[<[move_data.uuid]>]>
              #Check if previous tick is empty
              - if <[run_task.<[move_data.tick]>.run_task_list].is_empty>:
                - define run_task <[run_task].deep_exclude[<[move_data.tick]>]>
              #Set to new tick
              - define run_task.<[tick]>.<[move_data.uuid]>:<[move_data.data]>
              #Input to list
              - define run_task.<[tick]>.run_task_list:->:<[move_data.uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task:<[run_task]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[move_data.animator].replace[_].with[ ]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_prep:
              - define run_task_data <[keyframes.run_task.<[tick]>.<[uuid]>]>
              - definemap dup_data animator:run_task type:duplicate tick:<[tick]> uuid:<[uuid]> data:<[run_task_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this run task animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate:
              - define dup_data <player.flag[dcutscene_animator_change]>
              - define run_task <[keyframes.run_task]>
              #New uuid
              - define uuid <util.random_uuid>
              #Set to new tick
              - define run_task.<[tick]>.<[uuid]>:<[dup_data.data]>
              - define run_task.<[tick]>.run_task_list:->:<[uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task:<[run_task]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[dup_data.animator].replace[_].with[ ]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Play run task from this tick
            - case play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove run task
            - case remove:
              - flag <player> dcutscene_animator_change:!
              - define keyframe <[keyframes.run_task]>
              - define run_task <[keyframe.<[tick]>]>
              - define run_task_script <[run_task.<[uuid]>.script]>
              #Remove from run task list
              - define run_task.run_task_list:<-:<[uuid]>
              #If list is empty remove tick
              - if <[run_task.run_task_list].is_empty>:
                - define new_keyframe <[keyframe].deep_exclude[<[tick]>]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task:<[new_keyframe]>
              - else:
                - define new_keyframe <[run_task]>
                - define new_keyframe <[new_keyframe].deep_exclude[<[uuid]>]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.run_task.<[tick]>:<[new_keyframe]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - define text "Run task <green><[run_task_script]> <gray>has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

        #======== Cinematic Screeneffect Modifier =========
        - case screeneffect:
          - choose <[arg]>:
            #Prepare for new screeneffect
            - case new:
              - define effect_check <[keyframes.screeneffect.<[tick]>]||null>
              - if <[effect_check]> != null:
                - define text "There is already a cinematic screeneffect on tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - flag <player> cutscene_modify:screeneffect expire:2m
                - define text "Chat the screeneffect fade in, stay, and fade out like this <green>1s,5s,2s<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory close

            #Create new screeneffect
            - case create:
              - define split <[arg_2].split[,]>
              - define fade_in <duration[<[split].get[1]>]||null>
              - define stay <duration[<[split].get[2]>]||null>
              - define fade_out <duration[<[split].get[3]>]||null>
              - if <[fade_in]> != null && <[stay]> != null && <[fade_out]> != null:
                - flag <player> cutscene_modify:!
                - definemap screeneffect_data fade_in:<[fade_in]> stay:<[stay]> fade_out:<[fade_out]> color:black
                - flag server dcutscenes.<[data.name]>.keyframes.elements.screeneffect.<[tick]>:<[screeneffect_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Cinematic Screeneffect created at tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "Invalid input to specify fade in, stay, and fade out chat it like this <green>1s,5s,1s<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Prepare for presently new screeneffect time
            - case new_time:
              - flag <player> cutscene_modify:screeneffect_time expire:2m
              - define text "Chat the screeneffect fade in, stay, and fade out like this <green>1s,5s,2s<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory close

            #Set the new time
            - case set_time:
              - define split <[arg_2].split[,]>
              - define fade_in <duration[<[split].get[1]>]||null>
              - define stay <duration[<[split].get[2]>]||null>
              - define fade_out <duration[<[split].get[3]>]||null>
              - if <[fade_in]> != null && <[stay]> != null && <[fade_out]> != null:
                - flag <player> cutscene_modify:!
                - define effect_data <[keyframes.screeneffect.<[tick]>]>
                - define effect_data.fade_in <[fade_in]>
                - define effect_data.stay <[stay]>
                - define effect_data.fade_out <[fade_out]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.screeneffect.<[tick]>:<[effect_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - define text "New cinematic screeneffect time created at tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_screeneffect
              - else:
                - define text "Invalid input to specify fade in, stay, and fade out chat it like this <green>1s,5s,1s<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Prepare for present screeneffect new color
            - case new_color:
              - flag <player> cutscene_modify:screeneffect_color expire:1.5m
              - define text "Chat the color for the screeneffect you may also specify rgb as well Example: <green>blue <gray>or <green>150,39,255<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory close

            #Set new screeneffect color
            - case set_color:
              - define color <&color[<[arg_2]>]||null>
              - if <[color]> != null:
                - define keyframe <[keyframes.screeneffect.<[tick]>]>
                - define keyframe.color <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.screeneffect.<[tick]>:<[keyframe]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - define text "New cinematic screeneffect color created at tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_screeneffect
              - else:
                - define text "Invalid color."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Move to another keyframe preparation
            - case move_to_prep:
              - define screeneffect_data <[keyframes.screeneffect.<[tick]>]>
              - definemap move_data animator:screeneffect type:move tick:<[tick]> data:<[screeneffect_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this screeneffect animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move animator to another keyframe
            - case move_to:
              - if <[keyframes.screeneffect.<[tick]>]||null> == null:
                - define move_data <player.flag[dcutscene_animator_change]>
                #Remove previous tick
                - define screeneffect <[keyframes.screeneffect].deep_exclude[<[move_data.tick]>]>
                #Set previous screeneffect to new tick
                - define screeneffect.<[tick]> <[move_data.data]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.screeneffect:<[screeneffect]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "There is already a screeneffect at tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_prep:
              - define screeneffect_data <[keyframes.screeneffect.<[tick]>]>
              - definemap dup_data animator:screeneffect type:duplicate tick:<[tick]> data:<[screeneffect_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this screeneffect animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate:
              - if <[keyframes.screeneffect.<[tick]>]||null> == null:
                - define dup_data <player.flag[dcutscene_animator_change]>
                - define screeneffect <[keyframes.screeneffect]>
                - define screeneffect.<[tick]> <[dup_data.data]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.screeneffect:<[screeneffect]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[dup_data.animator]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "There is already a screeneffect at tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from here
            - case play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove the screeneffect modifier
            - case remove:
              - flag <player> dcutscene_animator_change:!
              - define keyframe <[keyframes.screeneffect].deep_exclude[<[tick]>]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.screeneffect:<[keyframe]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - define text "Cinematic Screeneffect has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

        #============== Sound Modifier ===============
        - case sound:
          - choose <[arg]>:
            #Prepare for sound creation
            - case new:
              - flag <player> cutscene_modify:sound expire:2.5m
              - define text "To add a sound to this keyframe do /dcutscene sound <green>my_sound<gray>. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory close

            #Create new sound
            - case create:
              - flag <player> cutscene_modify:!
              - if <server.sound_types.contains[<[arg_2]>]>:
                - playsound <player> sound:<[arg_2]>
              - define uuid <util.random_uuid>
              #Default values when creating new sound
              - definemap sound_data sound:<[arg_2]> volume:1.0 location:false pitch:1 custom:false
              #List of sounds by uuid
              - flag server dcutscenes.<[data.name]>.keyframes.elements.sound.<[tick]>.sounds:->:<[uuid]>
              #Set the sound data to the tick including the UUID
              - flag server dcutscenes.<[data.name]>.keyframes.elements.sound.<[tick]>.<[uuid]>:<[sound_data]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Sound <green><[arg_2]> <gray>has been added to tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Prepare for new volume
            - case new_volume:
              - flag <player> cutscene_modify:sound_volume expire:1.5m
              - define text "Chat the volume of the sound."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory close

            #Set new volume
            - case set_volume:
              - if <[arg_2].is_decimal>:
                - flag <player> cutscene_modify:!
                - define keyframe <[keyframes.sound.<[tick]>.<[uuid]>]||null>
                - if <[keyframe]> != null:
                  - define keyframe.volume <[arg_2].abs>
                  - flag server dcutscenes.<[data.name]>.keyframes.elements.sound.<[tick]>.<[uuid]>:<[keyframe]>
                  - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                  - define text "Sound <green><[keyframe.sound]> <gray>now has a volume of <green><[keyframe.volume].abs><gray> in tick <green><[tick]>t <gray> for scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - inventory open d:dcutscene_inventory_keyframe_modify_sound
                - else:
                  - debug error "Something went wrong in dcutscene_animator_keyframe_edit for set_volume in sound modifier"
              - else if !<[arg_2].is_decimal>:
                - define text "<green><[arg_2]> <gray>is not a number!"
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "Specify a number for the volume."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Prepare for new pitch
            - case new_pitch:
              - flag <player> cutscene_modify:sound_pitch expire:1.5m
              - define text "Chat the pitch of the sound."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory close

            #Set new pitch
            - case set_pitch:
              - if <[arg_2].is_decimal>:
                - flag <player> cutscene_modify:!
                - define keyframe <[keyframes.sound.<[tick]>.<[uuid]>]||null>
                - if <[keyframe]> != null:
                  - define keyframe.pitch <[arg_2].abs>
                  - flag server dcutscenes.<[data.name]>.keyframes.elements.sound.<[tick]>.<[uuid]>:<[keyframe]>
                  - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                  - define text "Sound <green><[keyframe.sound]> <gray>now has a pitch of <green><[keyframe.pitch].abs><gray> in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - inventory open d:dcutscene_inventory_keyframe_modify_sound
                - else:
                  - debug error "Something went wrong in dcutscene_animator_keyframe_edit for set_pitch in sound modifier"
              - else if && !<[arg_2].is_decimal>:
                - define text "<green><[arg_2]> <gray>is not a number!"
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "Specify a number for the pitch."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Determine if sound is custom or not
            - case set_custom:
              - define keyframe <[keyframes.sound.<[tick]>.<[uuid]>]||null>
              - if <[keyframe]> != null:
                - choose <[keyframe.custom]||false>:
                  - case true:
                    - define keyframe.custom false
                  - case false:
                    - define keyframe.custom true
                - flag server dcutscenes.<[data.name]>.keyframes.elements.sound.<[tick]>.<[uuid]>:<[keyframe]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define item <item[<[arg_2]>]||null>
                - if <[item]> != null:
                  - define inv <player.open_inventory>
                  - define lore "<dark_purple><bold>Custom <gray><[keyframe.custom]>"
                  - define click "<gray><italic>Click to change custom sound"
                  - adjust <[item]> lore:<list[<empty>|<[lore]>|<empty>|<[click]>]> save:item
                  - define item <entry[item].result>
                  - inventory set d:<[inv]> o:<[item]> slot:<[arg_3]>

            #Prepare for new sound location
            - case new_location:
              - flag <player> cutscene_modify:sound_location expire:3m
              - define text "Available Inputs:"
              - narrate "<[msg_prefix]> <gray><[text]>"
              - narrate "<gray>Chat <green>confirm <gray>to input your location"
              - narrate "<gray>Chat a valid location tag"
              - narrate "<gray>Right click a block"
              - narrate "<gray>Chat <red>false <gray>to disable sound location"
              - inventory close

            #Set new sound location
            - case set_location:
              - if <[arg_2]> != false:
                - define loc <location[<[arg_2].parsed>]||null>
                - if <[loc]> == null:
                  - define text "<green><[arg_2]> <gray>is not a valid location."
                  - narrate "<[msg_prefix]> <gray><[text]>"
                  - stop
              - else:
                - define loc false
              - define keyframe <[keyframes.sound.<[tick]>.<[uuid]>]||null>
              - if <[keyframe]> != null:
                - flag <player> cutscene_modify:!
                - define keyframe.location <[loc]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.sound.<[tick]>.<[uuid]>:<[keyframe]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define text "Sound <green><[keyframe.sound]> <gray>location is now <green><[keyframe.location]><gray> in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_keyframe_modify_sound
                - ~run dcutscene_origin_set def.scene:<[data.name]>

            #Move to prep
            - case move_to_prep:
              - define sound_data <[keyframes.sound.<[tick]>.<[uuid]>]>
              - definemap move_data animator:sound type:move tick:<[tick]> uuid:<[uuid]> data:<[sound_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this sound animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to
            - case move_to:
              - define move_data <player.flag[dcutscene_animator_change]>
              - define sound <[keyframes.sound]>
              #Update previous tick
              - define sound.<[move_data.tick]>.sounds:<-:<[move_data.uuid]>
              - define sound.<[move_data.tick]> <[sound.<[move_data.tick]>].deep_exclude[<[move_data.uuid]>]>
              #Check if previous tick is empty
              - if <[sound.<[move_data.tick]>.sounds].is_empty>:
                - define sound <[sound].deep_exclude[<[move_data.tick]>]>
              #Set to new tick
              - define sound.<[tick]>.<[move_data.uuid]>:<[move_data.data]>
              #Input to list
              - define sound.<[tick]>.sounds:->:<[move_data.uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.sound:<[sound]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_origin_set def.scene:<[data.name]>
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate
            - case duplicate_prep:
              - define sound_data <[keyframes.sound.<[tick]>.<[uuid]>]>
              - definemap dup_data animator:sound type:duplicate tick:<[tick]> uuid:<[uuid]> data:<[sound_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this run task animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate:
              - define dup_data <player.flag[dcutscene_animator_change]>
              - define sound <[keyframes.sound]>
              #New uuid
              - define uuid <util.random_uuid>
              #Set to new tick
              - define sound.<[tick]>.<[uuid]>:<[dup_data.data]>
              - define sound.<[tick]>.sounds:->:<[uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.sound:<[sound]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_origin_set def.scene:<[data.name]>
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[dup_data.animator].replace[_].with[ ]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from this tick
            - case play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove sound from tick
            - case remove_sound:
              - flag <player> dcutscene_animator_change:!
              - define sound <[keyframes.sound.<[tick]>.<[uuid]>.sound]>
              - define keyframe <[keyframes.sound]>
              #New modified data with sound removed
              - define new_keyframe <[keyframe.<[tick]>].deep_exclude[<[uuid]>]>
              - define new_keyframe.sounds:<-:<[uuid]>
              #If the animator list is empty for the tick remove the tick
              - if <[new_keyframe.sounds].is_empty>:
                #Keyframe with tick removed
                - define keyframe <[keyframes.sound].deep_exclude[<[tick]>]>
              - else:
                #Keyframe with tick intact due to present sounds in tick
                - define keyframe.<[tick]> <[new_keyframe]>
              - if <[new_keyframe]> != null:
                - flag server dcutscenes.<[data.name]>.keyframes.elements.sound:<[keyframe]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - define text "Sound <green><[sound]> <gray>has been removed from tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - else:
                - debug error "Something went wrong in dcutscene_animator_keyframe_edit for remove_sound in sound modifier"

        #============ Fake Block or Schematic Modifier =============
        - case fake_object:
          - choose <[arg]>:
            #====== Fake Block =======
            #Preparation for new fake block material
            - case new_fake_block_material:
              - define text "Input a material with /dcutscene material <green>example_block<gray>. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:fake_block_material expire:3m
              - inventory close

            #Prepare for new location
            - case new_fake_block_material_set:
              - define arg_2 <material[<[arg_2]>]||null>
              - define mat_check <material[<[arg_2]>].is_block||false>
              - if <[arg_2]> != null || <[mat_check].is_truthy>:
                - flag <player> dcutscene_save_data.block:<[arg_2]>
                - define text "Right click on the block you'd like to fake show to or chat a valid location tag. Chat <red>cancel <gray>to stop."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:fake_block_location expire:5m
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid material."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Create new fake block based on location input
            - case new_fake_block_loc:
              - define arg_2 <location[<[arg_2]>]||null>
              - if <[arg_2]> != null:
                - flag <player> cutscene_modify:!
                - define uuid <util.random_uuid>
                #Set the fake block list
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_block.<[tick]>.fake_blocks:->:<[uuid]>
                #Set the new data
                - definemap proc_data script:none defs:none
                - definemap fake_block_data loc:<[arg_2]> block:<player.flag[dcutscene_save_data.block]> procedure:<[proc_data]> duration:10s
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_block.<[tick]>.<[uuid]>:<[fake_block_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - define text "Fake block <green><player.flag[dcutscene_save_data.block].name> <gray>set at location <green><[arg_2].simple> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_origin_set def.scene:<[data.name]>
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid location."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Preparation for setting a new fake block location
            - case set_fake_block_prepare:
              - define text "Right click on the block you'd like to fake show to or chat a valid location tag. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:set_fake_block_location expire:5m
              - inventory close

            #Set a new fake block location in the already created keyframe
            - case set_fake_block_loc:
              - define arg_2 <location[<[arg_2]>]||null>
              - if <[arg_2]> != null:
                - flag <player> cutscene_modify:!
                - define fake_block <[keyframes.fake_object.fake_block.<[tick]>.<[uuid]>]>
                - define fake_block.loc <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_block.<[tick]>.<[uuid]>:<[fake_block]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_fake_object_block_modify
                - ~run dcutscene_origin_set def.scene:<[data.name]>
                - define text "Fake block <green><[fake_block.block].name> <gray>on tick <green><[tick]>t <gray>location has been changed to <green><[arg_2].simple> <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid location."

            #Preparation for setting a new fake block material
            - case set_fake_block_material_prep:
              - define text "Input a material with /dcutscene material <green>example_block<gray>. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:set_fake_block_material expire:3m
              - inventory close

            #Set the new fake block material
            - case set_fake_block_material:
              - define arg_2 <material[<[arg_2]>]||null>
              - define mat_check <material[<[arg_2]>].is_block||false>
              - if <[arg_2]> != null || <[mat_check].is_truthy>:
                - flag <player> cutscene_modify:!
                - define fake_block <[keyframes.fake_object.fake_block.<[tick]>.<[uuid]>]>
                - define fake_block.block <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_block.<[tick]>.<[uuid]>:<[fake_block]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_fake_object_block_modify
                - define text "Fake block <[fake_block.block].name> on tick <green><[tick]>t <gray>material has been changed to <green><[arg_2].name> <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid material."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Procedure script preparation
            - case set_fake_block_proc_prepare:
              - define text "Chat the name of the procedure script. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:fake_block_proc_script expire:2m
              - inventory close

            #Set procedure script
            - case set_fake_block_proc:
              - define proc_check <player.location.proc[<[arg_2]>]||null>
              - if <[proc_check]> != null:
                - flag <player> cutscene_modify:!
                - define fake_block <[keyframes.fake_object.fake_block.<[tick]>.<[uuid]>]>
                - define fake_block.procedure.script <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_block.<[tick]>.<[uuid]>:<[fake_block]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_fake_object_block_modify
                - define text "Fake block <green><[fake_block.block].name> <gray>on tick <green><[tick]>t <gray>procedure script is now <green><[arg_2]> <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid procedure script."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Procedure definition preparation
            - case set_fake_block_proc_def_prepare:
              - define text "Chat the definitions for the procedure script. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:fake_block_proc_def expire:5m
              - inventory close

            #Set the procedure definitions
            - case set_fake_block_proc_def:
              - flag <player> cutscene_modify:!
              - define fake_block <[keyframes.fake_object.fake_block.<[tick]>.<[uuid]>]>
              - define fake_block.procedure.defs <[arg_2]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_block.<[tick]>.<[uuid]>:<[fake_block]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_fake_object_block_modify
              - define text "Fake block <green><[fake_block.block].name> <gray>on tick <green><[tick]>t <gray>procedure definition is now <green><[arg_2]> <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Fake block duration
            - case set_fake_block_duration_prepare:
              - define text "Chat the duration of the fake block. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:fake_block_duration expire:2m
              - inventory close

            #Set fake block duration
            - case set_fake_block_duration:
              - define duration_check <duration[<[arg_2]>]||null>
              - if <[duration_check]> != null:
                - flag <player> cutscene_modify:!
                - define fake_block <[keyframes.fake_object.fake_block.<[tick]>.<[uuid]>]>
                - define fake_block.duration <duration[<[arg_2]>]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_block.<[tick]>.<[uuid]>:<[fake_block]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_fake_object_block_modify
                - define text "Fake block <green><[fake_block.block].name> <gray>on tick <green><[tick]>t <gray>duration is now <green><duration[<[arg_2]>].formatted> <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid duration."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Teleport to location
            - case teleport_to_fake_block:
              - define loc <[keyframes.fake_object.fake_block.<[tick]>.<[uuid]>.loc]>
              - teleport <player> <[loc]>
              - define text "You have teleported to fake block location <green><[loc].simple> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory open d:dcutscene_inventory_fake_object_block_modify

            #Move to new keyframe prep
            - case move_to_fake_block_prep:
              - define fake_block_data <[keyframes.fake_object.fake_block.<[tick]>.<[uuid]>]>
              - definemap move_data animator:fake_block type:move tick:<[tick]> uuid:<[uuid]> data:<[fake_block_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this fake block animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to new keyframe
            - case move_to_fake_block:
              - define move_data <player.flag[dcutscene_animator_change]>
              - define fake_block <[keyframes.fake_object.fake_block.<[tick]>]>
              #Update previous tick
              - define fake_block.<[move_data.tick]>.fake_blocks:<-:<[move_data.uuid]>
              - define fake_block.<[move_data.tick]> <[fake_block.<[move_data.tick]>].deep_exclude[<[move_data.uuid]>]>
              #Check if previous tick is empty
              - if <[fake_block.<[move_data.tick]>.fake_blocks].is_empty>:
                - define fake_block <[fake_block].deep_exclude[<[move_data.tick]>]>
              #Set to new tick
              - define fake_block.<[tick]>.<[move_data.uuid]>:<[move_data.data]>
              #Input to list
              - define fake_block.<[tick]>.fake_blocks:->:<[move_data.uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_block:<[fake_block]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_origin_set def.scene:<[data.name]>
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[move_data.animator].replace[_].with[ ]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_fake_block_prep:
              - define fake_block_data <[keyframes.fake_object.fake_block.<[tick]>.<[uuid]>]>
              - definemap dup_data animator:fake_block type:duplicate tick:<[tick]> uuid:<[uuid]> data:<[fake_block_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this fake block animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate_fake_block:
              - define dup_data <player.flag[dcutscene_animator_change]>
              - define fake_block <[keyframes.fake_object.fake_block]>
              #New uuid
              - define uuid <util.random_uuid>
              #Set to new tick
              - define fake_block.<[tick]>.<[uuid]>:<[dup_data.data]>
              - define fake_block.<[tick]>.fake_blocks:->:<[uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_block:<[fake_block]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_origin_set def.scene:<[data.name]>
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[dup_data.animator].replace[_].with[ ]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from here
            - case fake_block_play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove the fake block
            - case remove_fake_block:
              - flag <player> dcutscene_animator_change:!
              #Data
              - define fake_object <[keyframes.fake_object]>
              #Block name
              - define fake_block_name <[fake_object.fake_block.<[tick]>.<[uuid]>.block].name>
              #Remove uuid from list
              - define fake_object.fake_block.<[tick]>.fake_blocks:<-:<[uuid]>
              #Remove the uuid
              - define fake_object.fake_block.<[tick]> <[fake_object.fake_block.<[tick]>].deep_exclude[<[uuid]>]>
              #If list is empty remove tick
              - if <[fake_object.fake_block.<[tick]>.fake_blocks].is_empty>:
                - define fake_object.fake_block <[fake_object.fake_block].deep_exclude[<[tick]>]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object:<[fake_object]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Fake block <green><[fake_block_name]> <gray>has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
            #=================================

            #======== Fake Schematic =========
            #Preparation for schematic
            - case new_schem_name:
              - define text "Chat the name of the schematic. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:new_fake_schem_name expire:2m
              - inventory close

            #Input schematic name
            - case new_schem_loc:
              - if <schematic[<[arg_2]>].exists>:
                - flag <player> cutscene_modify:new_fake_schem_loc expire:5m
                - flag <player> dcutscene_save_data.schem_name:<[arg_2]>
                - define text "Right click on the block you'd like to paste this schematic or chat a valid location tag the location's center will automatically be parsed. Chat <red>cancel <gray>to stop."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "Could not find schematic <green><[arg_2]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Set the new fake schematic
            - case new_schem_create:
              - if <location[<[arg_2]>]||null> != null:
                - flag <player> cutscene_modify:!
                - define ran_uuid <util.random_uuid>
                #Set the uuid into the list
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem.<[tick]>.fake_schems:->:<[ran_uuid]>
                #Set the new data
                - definemap fake_schem_data schem:<player.flag[dcutscene_save_data.schem_name]> loc:<[arg_2]> duration:10s noair:true waitable:false angle:forward mask:false
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem.<[tick]>.<[ran_uuid]>:<[fake_schem_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - define text "Fake schematic <green><player.flag[dcutscene_save_data.schem_name]> <gray>set at location <green><[arg_2].simple> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_origin_set def.scene:<[data.name]>
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid location."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Name change prepare
            - case change_schem_name_prep:
              - define text "Chat the name of the new schematic. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_fake_schem_name expire:2m
              - inventory close

            #Change schem name
            - case change_schem_name:
              - if <schematic[<[arg_2]>].exists>:
                - flag <player> cutscene_modify:!
                - define fake_schem <[keyframes.fake_object.fake_schem.<[tick]>.<[uuid]>]>
                - define pre_name <[fake_schem.schem]>
                - define fake_schem.schem <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem.<[tick]>.<[uuid]>:<[fake_schem]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_fake_object_schem_modify
                - define text "Fake schematic <green><[pre_name]> <gray>name is now <green><[fake_schem.schem]> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "Could not find schematic <green><[arg_2]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change Schem Location Prepare
            - case change_schem_loc_prep:
              - define text "Right click on the block you'd like to paste this schematic or chat a valid location tag the location's center will automatically be parsed. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_fake_schem_loc expire:5m
              - inventory close

            #Change Schem Location
            - case change_schem_loc:
              - if <location[<[arg_2]>]||null> != null:
                - flag <player> cutscene_modify:!
                - define fake_schem <[keyframes.fake_object.fake_schem.<[tick]>.<[uuid]>]>
                - define fake_schem.loc <location[<[arg_2]>]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem.<[tick]>.<[uuid]>:<[fake_schem]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_fake_object_schem_modify
                - define text "Fake schematic <green><[fake_schem.schem]> <gray>location is now <green><[arg_2].simple> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - ~run dcutscene_origin_set def.scene:<[data.name]>
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid location."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change Schem Duration Prep
            - case change_schem_duration_prep:
              - define text "Chat the duration this schematic will appear for. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_fake_schem_duration expire:2.5m
              - inventory close

            #Change Schem Duration
            - case change_schem_duration:
              - define duration_check <duration[<[arg_2]>]||null>
              - if <[duration_check]> != null:
                - flag <player> cutscene_modify:!
                - define fake_schem <[keyframes.fake_object.fake_schem.<[tick]>.<[uuid]>]>
                - define fake_schem.duration <duration[<[arg_2]>]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem.<[tick]>.<[uuid]>:<[fake_schem]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_fake_object_schem_modify
                - define text "Fake schematic <green><[fake_schem.schem]> <gray>duration is now <green><duration[<[arg_2]>].formatted> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid duration."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change Schem Noair
            - case change_schem_noair:
              - define fake_schem <[keyframes.fake_object.fake_schem.<[tick]>.<[uuid]>]>
              - choose <[fake_schem.noair]>:
                - case true:
                  - define fake_schem.noair false
                - case false:
                  - define fake_schem.noair true
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem.<[tick]>.<[uuid]>:<[fake_schem]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - define l1 "<aqua>No air: <gray><[fake_schem.noair]>"
              - define click "<gray><italic>Click to change schematic noair"
              - define lore <list[<empty>|<[l1]>|<empty>|<[click]>]>
              - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

            #Change schem waitable
            - case change_schem_waitable:
              - define fake_schem <[keyframes.fake_object.fake_schem.<[tick]>.<[uuid]>]>
              - choose <[fake_schem.waitable]>:
                - case true:
                  - define fake_schem.waitable false
                - case false:
                  - define fake_schem.waitable true
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem.<[tick]>.<[uuid]>:<[fake_schem]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - define l1 "<gold>Waitable: <gray><[fake_schem.waitable]>"
              - define click "<gray><italic>Click to change schematic waitable"
              - define lore <list[<empty>|<[l1]>|<empty>|<[click]>]>
              - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

            #Change schem direction
            - case change_schem_direction:
              - define fake_schem <[keyframes.fake_object.fake_schem.<[tick]>.<[uuid]>]>
              - choose <[fake_schem.angle]||forward>:
                - case forward:
                  - define fake_schem.angle backward
                - case backward:
                  - define fake_schem.angle right
                - case right:
                  - define fake_schem.angle left
                - case left:
                  - define fake_schem.angle forward
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem.<[tick]>.<[uuid]>:<[fake_schem]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - define l1 "<dark_blue>Direction: <gray><[fake_schem.angle]>"
              - define click "<gray><italic>Click to change schematic paste direction"
              - define lore <list[<empty>|<[l1]>|<empty>|<[click]>]>
              - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

            #Teleport to fake schem location
            - case teleport_to_fake_schem:
              - define fake_schem <[keyframes.fake_object.fake_schem.<[tick]>.<[uuid]>]>
              - teleport <player> <[fake_schem.loc]>
              - define text "You have teleported to fake schematic location <green><[fake_schem.loc].simple> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory open d:dcutscene_inventory_fake_object_schem_modify

            #Move to new keyframe prep
            - case move_to_fake_schem_prep:
              - define fake_schem_data <[keyframes.fake_object.fake_schem.<[tick]>.<[uuid]>]>
              - definemap move_data animator:fake_schem type:move tick:<[tick]> uuid:<[uuid]> data:<[fake_schem_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this fake schematic animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to new keyframe
            - case move_to_fake_schem:
              - define move_data <player.flag[dcutscene_animator_change]>
              - define fake_schem <[keyframes.fake_object.fake_schem.<[tick]>]>
              #Update previous tick
              - define fake_schem.<[move_data.tick]>.fake_schems:<-:<[move_data.uuid]>
              - define fake_schem.<[move_data.tick]> <[fake_schem.<[move_data.tick]>].deep_exclude[<[move_data.uuid]>]>
              #Check if previous tick is empty
              - if <[fake_schem.<[move_data.tick]>.fake_schems].is_empty>:
                - define fake_schem <[fake_schem].deep_exclude[<[move_data.tick]>]>
              #Set to new tick
              - define fake_schem.<[tick]>.<[move_data.uuid]>:<[move_data.data]>
              #Input to list
              - define fake_schem.<[tick]>.fake_schems:->:<[move_data.uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem:<[fake_schem]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_origin_set def.scene:<[data.name]>
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[move_data.animator].replace[_].with[ ]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_fake_schem_prep:
              - define fake_schem_data <[keyframes.fake_object.fake_schem.<[tick]>.<[uuid]>]>
              - definemap dup_data animator:fake_schem type:duplicate tick:<[tick]> uuid:<[uuid]> data:<[fake_schem_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this fake schematic animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate_fake_schem:
              - define dup_data <player.flag[dcutscene_animator_change]>
              - define fake_schem <[keyframes.fake_object.fake_schem]>
              #New uuid
              - define uuid <util.random_uuid>
              #Set to new tick
              - define fake_schem.<[tick]>.<[uuid]>:<[dup_data.data]>
              - define fake_schem.<[tick]>.fake_schems:->:<[uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object.fake_schem:<[fake_schem]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_origin_set def.scene:<[data.name]>
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[dup_data.animator].replace[_].with[ ]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from here
            - case fake_schem_play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove fake schem
            - case remove_fake_schem:
              - flag <player> dcutscene_animator_change:!
              #Data
              - define fake_object <[keyframes.fake_object]>
              #Schem name
              - define fake_schem <[fake_object.fake_schem.<[tick]>.<[uuid]>.schem]>
              #Remove uuid from list
              - define fake_object.fake_schem.<[tick]>.fake_schems:<-:<[uuid]>
              #Remove the uuid
              - define fake_object.fake_schem.<[tick]> <[fake_object.fake_schem.<[tick]>].deep_exclude[<[uuid]>]>
              #If list is empty remove tick
              - if <[fake_object.fake_schem.<[tick]>.fake_schems].is_empty>:
                - define fake_object.fake_schem <[fake_object.fake_schem].deep_exclude[<[tick]>]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.fake_object:<[fake_object]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Fake schematic <green><[fake_schem]> <gray>has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
            #============================
            #===========================================

        #============= Particle Modifier ===============
        - case particle:
          - choose <[arg]>:
            #Preparation for new particle
            - case new_particle_prep:
              - define text "To add a new particle use the command /dcutscene particle <green>my_particle<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:new_particle expire:5m
              - inventory close

            #Set the new particle and prepare for new location
            - case new_particle_loc:
              - if <server.particle_types.contains[<[arg_2]>]>:
                - flag <player> dcutscene_save_data.particle:<[arg_2]>
                - flag <player> cutscene_modify:new_particle_loc expire:5m
                - define text "Right click on the block you'd like this particle to be at or chat a valid LocationTag. Chat <red>cancel <gray>to stop."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid particle."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Create new particle
            - case new_particle:
              - if <location[<[arg_2]>]||null> != null:
                - flag <player> cutscene_modify:!
                - define ran_uuid <util.random_uuid>
                #Set uuid into particle list
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.particle_list:->:<[ran_uuid]>
                #Set the new data
                - definemap particle_proc script:false defs:false
                - definemap particle_data particle:<player.flag[dcutscene_save_data.particle]> loc:<location[<[arg_2]>]> range:100 quantity:1 offset:0,0,0 repeat:1 repeat_interval:<duration[1s]> velocity:0,0,0 special_data:false procedure:<[particle_proc]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[ran_uuid]>:<[particle_data]>
                #Update
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - define text "Particle <green><player.flag[dcutscene_save_data.particle]> <gray>set at location <green><[arg_2].simple> <gray>in tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_origin_set def.scene:<[data.name]>
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid location."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change Particle Prep
            - case change_particle_prep:
              - define text "To add a new particle use the command /dcutscene particle <green>my_particle<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle expire:5m
              - inventory close

            #Change Particle
            - case change_particle:
              - if <server.particle_types.contains[<[arg_2]>]>:
                - flag <player> cutscene_modify:!
                - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
                - define particle.particle <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_particle_modify
                - define text "Particle animator in tick <green><[tick]>t <gray>particle is now <green><[arg_2]> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid particle."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change Location Prep
            - case change_particle_loc_prep:
              - define text "Right click on the block you'd like this particle to be at or chat a valid LocationTag. Chat <red>cancel <gray>to stop."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_loc expire:5m
              - inventory close

            #Change Location
            - case change_particle_loc:
              - if <location[<[arg_2]>]||null> != null:
                - flag <player> cutscene_modify:!
                - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
                - define particle.loc <location[<[arg_2]>]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_particle_modify
                - define text "Particle animator in tick <green><[tick]>t <gray>location is now <green><[arg_2].simple> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - ~run dcutscene_origin_set def.scene:<[data.name]>
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid location."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change Quantity Prep
            - case change_particle_quantity_prep:
              - define text "Chat the quantity of particles that will be played."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_quantity expire:2m
              - inventory close

            #Change Quanity
            - case change_particle_quantity:
              - if <[arg_2].is_integer>:
                - flag <player> cutscene_modify:!
                - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
                - define particle.quantity <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_particle_modify
                - define text "Particle animator in tick <green><[tick]>t <gray>quantity is now <green><[arg_2]> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not an integer or number."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change Range Prep
            - case change_particle_range_prep:
              - define text "Chat the visible range the particle can be seen at."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_range expire:2m
              - inventory close

            #Change visible range
            - case change_particle_range:
              - if <[arg_2].is_integer>:
                - flag <player> cutscene_modify:!
                - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
                - define particle.range <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_particle_modify
                - define text "Particle animator in tick <green><[tick]>t <gray>visibility range is now <green><[arg_2]> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not an integer or number."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change repeat count prep
            - case change_particle_repeat_count_prep:
              - define text "Chat the repeat count for the particle."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_repeat_count expire:2m
              - inventory close

            #Change repeat count
            - case change_particle_repeat_count:
              - if <[arg_2].is_integer>:
                - if <[arg_2]> < 1:
                  - define arg_2 1
                - flag <player> cutscene_modify:!
                - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
                - define particle.repeat <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_particle_modify
                - define text "Particle animator in tick <green><[tick]>t <gray>repeat count is now <green><[arg_2]> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not an integer or number."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change repeat interval prep
            - case change_particle_repeat_interval_prep:
              - define text "Chat the repeat interval for the particle."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_repeat_interval expire:2m
              - inventory close

            #Change repeat interval
            - case change_particle_repeat_interval:
              - if <duration[<[arg_2]>]||null> != null:
                - flag <player> cutscene_modify:!
                - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
                - define particle.repeat_interval <duration[<[arg_2]>]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_particle_modify
                - define text "Particle animator in tick <green><[tick]>t <gray>repeat interval is now <green><[arg_2]> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not an integer or number."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change offset prep
            - case change_particle_offset_prep:
              - define text "Chat the particle offset in the form 0,0,0."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_offset expire:2m
              - inventory close

            #Change offset
            - case change_particle_offset:
              - if <location[<[arg_2]>]||null> != null:
                - flag <player> cutscene_modify:!
                - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
                - define particle.offset <location[<[arg_2]>].xyz>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_particle_modify
                - define text "Particle animator in tick <green><[tick]>t <gray>particle offset is now <green><location[<[arg_2]>].xyz> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is an invalid offset."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change procedure script prep
            - case change_particle_procedure_script_prep:
              - define text "Chat the name of the procedure script for this particle."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_proc_script expire:2m
              - inventory close

            #Change procedure script
            - case change_particle_procedure_script:
              - if <proc[<[arg_2]>]||null> != null:
                - flag <player> cutscene_modify:!
                - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
                - define particle.procedure.script <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_particle_modify
                - define text "Particle animator in tick <green><[tick]>t <gray>procedure script is now <green><[arg_2]> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid procedure script."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change procedure definitions prep
            - case change_particle_procedure_defs_prep:
              - define text "Chat the definition for the procedure script."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_proc_defs expire:2m
              - inventory close

            #Change procedure definitions
            - case change_particle_procedure_defs:
              - flag <player> cutscene_modify:!
              - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
              - define particle.procedure.defs <[arg_2]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_particle_modify
              - define text "Particle animator in tick <green><[tick]>t <gray>procedure definition is now <green><[arg_2]> <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Change special data prep
            - case change_particle_special_data_prep:
              - define text "Chat the special data for the particle."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_special_data expire:3m
              - inventory close

            #Change special data
            - case change_particle_special_data:
              - flag <player> cutscene_modify:!
              - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
              - define particle.special_data <[arg_2]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_particle_modify
              - define text "Particle animator in tick <green><[tick]>t <gray>special data is now <green><[arg_2]> <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Change velocity prep
            - case change_particle_velocity_prep:
              - define text "Chat a valid velocity vector it can be a LocationTag or the input as <green>0,0,0<gray>. Chat <green>false <gray>to disable."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_particle_velocity expire:3m
              - inventory close

            #Change velocity
            - case change_particle_velocity:
              - if <location[<[arg_2]>]||null> != null:
                - flag <player> cutscene_modify:!
                - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
                - define particle.velocity <location[<[arg_2]>].xyz>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.particle.<[tick]>.<[uuid]>:<[particle]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_particle_modify
                - define text "Particle animator in tick <green><[tick]>t <gray>velocity is now <green><location[<[arg_2]>].xyz> <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid velocity vector."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Teleport to particle location
            - case teleport_to_particle:
              - define particle <[keyframes.particle.<[tick]>.<[uuid]>]>
              - define loc <location[<[particle.loc]>]>
              - teleport <player> <[loc]>
              - define text "You have teleported to particle <green><[particle.particle]> <gray>location at <green><[loc].simple> <gray>in tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory open d:dcutscene_inventory_particle_modify

            #Move to new keyframe prep
            - case move_to_prep:
              - define particle_data <[keyframes.particle.<[tick]>.<[uuid]>]>
              - definemap move_data animator:particle type:move tick:<[tick]> uuid:<[uuid]> data:<[particle_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this particle animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to
            - case move_to:
              - define move_data <player.flag[dcutscene_animator_change]>
              - define particle <[keyframes.particle]>
              #Update previous tick
              - define particle.<[move_data.tick]>.particle_list:<-:<[move_data.uuid]>
              - define particle.<[move_data.tick]> <[particle.<[move_data.tick]>].deep_exclude[<[move_data.uuid]>]>
              #Check if previous tick is empty
              - if <[particle.<[move_data.tick]>.particle_list].is_empty>:
                - define particle <[particle].deep_exclude[<[move_data.tick]>]>
              #Set to new tick
              - define particle.<[tick]>.<[move_data.uuid]>:<[move_data.data]>
              #Input to list
              - define particle.<[tick]>.particle_list:->:<[move_data.uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.particle:<[particle]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_origin_set def.scene:<[data.name]>
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_prep:
              - define particle_data <[keyframes.particle.<[tick]>.<[uuid]>]>
              - definemap dup_data animator:particle type:duplicate tick:<[tick]> uuid:<[uuid]> data:<[particle_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this particle animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate:
              - define dup_data <player.flag[dcutscene_animator_change]>
              - define particle <[keyframes.particle]>
              #New uuid
              - define uuid <util.random_uuid>
              #Set to new tick
              - define particle.<[tick]>.<[uuid]>:<[dup_data.data]>
              - define particle.<[tick]>.particle_list:->:<[uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.particle:<[particle]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_origin_set def.scene:<[data.name]>
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[dup_data.animator].replace[_].with[ ]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from here
            - case play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove Particle
            - case remove_particle:
              - flag <player> dcutscene_animator_change:!
              #Data
              - define particle <[keyframes.particle]>
              #Particle name
              - define particle_name <[particle.<[tick]>.<[uuid]>.particle]>
              #Remove uuid from list
              - define particle.<[tick]>.particle_list:<-:<[uuid]>
              #Remove the uuid
              - define particle.<[tick]> <[particle.<[tick]>].deep_exclude[<[uuid]>]>
              #If list is empty remove tick
              - if <[particle.<[tick]>.particle_list].is_empty>:
                - define particle <[particle].deep_exclude[<[tick]>]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.particle:<[particle]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Particle <green><[particle_name]> <gray>has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

        #======== Title Modifier ========
        - case title:
          - choose <[arg]>:
            #New title
            - case new_title:
              #Title check
              - if <[keyframes.title.<[tick]>]||null> != null:
                - define text "There is already a title on tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - stop
              - definemap title_data title:Hello! subtitle:<empty> fade_in:1s stay:3s fade_out:1s
              - flag server dcutscenes.<[data.name]>.keyframes.elements.title.<[tick]>:<[title_data]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - ~run dcutscene_sort_data def:<[data.name]>
              - define text "Title set on tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory open d:dcutscene_inventory_keyframe_modify_title

            #Set title prep
            - case set_title_prep:
              - define text "Chat the title. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_title expire:4m
              - inventory close

            #Set title
            - case set_title:
              - flag <player> cutscene_modify:!
              - define title <[keyframes.title.<[tick]>]>
              - define title.title <[arg_2]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.title.<[tick]>:<[title]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_keyframe_modify_title
              - define text "Title on tick <green><[tick]>t <gray>title message is now <[arg_2].parse_color> <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Set subtitle prep
            - case set_subtitle_prep:
              - define text "Chat the subtitle. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_subtitle expire:5m
              - inventory close

            #Set subtitle
            - case set_subtitle:
              - flag <player> cutscene_modify:!
              - define title <[keyframes.title.<[tick]>]>
              - define title.subtitle <[arg_2]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.title.<[tick]>:<[title]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_keyframe_modify_title
              - define text "Title on tick <green><[tick]>t <gray>subtitle message is now <[arg_2].parse_color> <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Set Duration prep
            - case set_duration_prep:
              - define text "Chat the title fade in, stay, and fade out like this <green>1s,3s,1s<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_title_duration expire:3m
              - inventory close

            #Set Duration
            - case set_duration:
              - flag <player> cutscene_modify:!
              - define split <[arg_2].split[,]>
              - define fade_in <duration[<[split].get[1]>]||null>
              - define stay <duration[<[split].get[2]>]||null>
              - define fade_out <duration[<[split].get[3]>]||null>
              - if <[fade_in]> != null && <[stay]> != null && <[fade_out]> != null:
                - define title <[keyframes.title.<[tick]>]>
                - definemap title_data fade_in:<[fade_in]> stay:<[stay]> fade_out:<[fade_out]>
                - define title <[title_data]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.title.<[tick]>:<[title]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_keyframe_modify_title
              - define text "Title on tick <green><[tick]>t <gray>fade in, stay, and fade out is now <green><[fade_in].formatted>,<[stay].formatted>,<[fade_out].formatted> <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Move to prep
            - case move_to_prep:
              - define title_data <[keyframes.title.<[tick]>]>
              - definemap move_data animator:title type:move tick:<[tick]> data:<[title_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this title animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to new keyframe
            - case move_to:
              - if <[keyframes.title.<[tick]>]||null> == null:
                - define move_data <player.flag[dcutscene_animator_change]>
                #Remove previous tick
                - define title <[keyframes.title].deep_exclude[<[move_data.tick]>]>
                #Set previous screeneffect to new tick
                - define title.<[tick]> <[move_data.data]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.title:<[title]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "There is already a title at tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_prep:
              - define title_data <[keyframes.title.<[tick]>]>
              - definemap dup_data animator:title type:duplicate tick:<[tick]> data:<[title_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this title animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate:
              - if <[keyframes.title.<[tick]>]||null> == null:
                - define dup_data <player.flag[dcutscene_animator_change]>
                - define title <[keyframes.title]>
                - define title.<[tick]> <[dup_data.data]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.title:<[title]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[dup_data.animator]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "There is already a title at tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from here
            - case play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove title
            - case remove_title:
              - flag <player> dcutscene_animator_change:!
              - define title_msg <[keyframes.title.<[tick]>.title].parse_color>
              - define title <[keyframes.title].deep_exclude[<[tick]>]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.title:<[title]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Title <[title_msg]> <gray>has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

        #========== Command Modifier ==========
        - case command:
          - choose <[arg]>:
            #Preparation for new command
            - case new_command_prep:
              - define text "Chat the command that will be executed. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:new_command expire:5m
              - inventory close

            #New command
            - case new_command:
              - flag <player> cutscene_modify:!
              - define ran_uuid <util.random_uuid>
              - definemap command_data command:<[arg_2]> execute_as:player silent:false
              #Set list of commands within tick
              - flag server dcutscenes.<[data.name]>.keyframes.elements.command.<[tick]>.command_list:->:<[ran_uuid]>
              #Set new data
              - definemap command_data command:<[arg_2]> execute_as:player silent:false
              - flag server dcutscenes.<[data.name]>.keyframes.elements.command.<[tick]>.<[ran_uuid]>:<[command_data]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - ~run dcutscene_sort_data def:<[data.name]>
              - define text "Command <green><[arg_2]> <gray>set at tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

            #Change command prep
            - case change_command_prep:
              - define text "Chat the command that will be executed. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_command expire:5m
              - inventory close

            #Change command
            - case change_command:
              - flag <player> cutscene_modify:!
              - define command <[keyframes.command.<[tick]>.<[uuid]>]>
              - define command.command <[arg_2]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.command.<[tick]>.<[uuid]>:<[command]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_keyframe_modify_command
              - define text "Command on tick <green><[tick]>t <gray>command to be executed is now <green><[arg_2]> <gray>for scene <green><[data.name]>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Change command execute_as
            - case change_execute_as:
              - define command <[keyframes.command.<[tick]>.<[uuid]>]>
              - choose <[command.execute_as]||player>:
                - case player:
                  - define command.execute_as server
                - case server:
                  - define command.execute_as player
              - flag server dcutscenes.<[data.name]>.keyframes.elements.command.<[tick]>.<[uuid]>:<[command]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - define l1 "<red>Execute as: <gray><[command.execute_as]>"
              - define l2 "<gray><italic>Click to set execute as"
              - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
              - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

            #Change command silent
            - case change_silent:
              - define command <[keyframes.command.<[tick]>.<[uuid]>]>
              - choose <[command.silent]>:
                - case true:
                  - define command.silent false
                - case false:
                  - define command.silent true
              - flag server dcutscenes.<[data.name]>.keyframes.elements.command.<[tick]>.<[uuid]>:<[command]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_keyframe_modify_command
              - define l1 "<dark_gray>Silent: <gray><[command.silent]>"
              - define l2 "<gray><italic>Click to set silent command"
              - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
              - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

            #Move to new keyframe prep
            - case move_to_prep:
              - define command_data <[keyframes.command.<[tick]>.<[uuid]>]>
              - definemap move_data animator:command type:move tick:<[tick]> uuid:<[uuid]> data:<[command_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this command animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to
            - case move_to:
              - define move_data <player.flag[dcutscene_animator_change]>
              - define command <[keyframes.command.<[tick]>]>
              #Update previous tick
              - define command.<[move_data.tick]>.command_list:<-:<[move_data.uuid]>
              - define command.<[move_data.tick]> <[command.<[move_data.tick]>].deep_exclude[<[move_data.uuid]>]>
              #Check if previous tick is empty
              - if <[command.<[move_data.tick]>.command_list].is_empty>:
                - define command <[command].deep_exclude[<[move_data.tick]>]>
              #Set to new tick
              - define command.<[tick]>.<[move_data.uuid]>:<[move_data.data]>
              #Input to list
              - define command.<[tick]>.command_list:->:<[move_data.uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.command:<[command]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_prep:
              - define command_data <[keyframes.command.<[tick]>.<[uuid]>]>
              - definemap dup_data animator:command type:duplicate tick:<[tick]> uuid:<[uuid]> data:<[command_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to move this command animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate:
              - define dup_data <player.flag[dcutscene_animator_change]>
              - define command <[keyframes.command]>
              #New uuid
              - define uuid <util.random_uuid>
              #Set to new tick
              - define command.<[tick]>.<[uuid]>:<[dup_data.data]>
              - define command.<[tick]>.command_list:->:<[uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.command:<[command]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[dup_data.animator].replace[_].with[ ]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from this tick
            - case play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove command
            - case remove_command:
              - flag <player> dcutscene_animator_change:!
              - define command <[keyframes.command]>
              #Remove uuid from list
              - define command.<[tick]>.command_list:<-:<[uuid]>
              #Remove the uuid
              - define command.<[tick]> <[command.<[tick]>].deep_exclude[<[uuid]>]>
              #If list is empty remove tick
              - if <[command.<[tick]>.command_list].is_empty>:
                - define command <[command].deep_exclude[<[tick]>]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.command:<[command]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Command has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

        #========= Message Modifier ==========
        - case message:
          - choose <[arg]>:
            #Prep for new message
            - case new_message_prep:
              - define text "Chat the message. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:new_message expire:5m
              - inventory close

            #New message
            - case new_message:
              - flag <player> cutscene_modify:!
              - define ran_uuid <util.random_uuid>
              #Set list of messages in tick
              - flag server dcutscenes.<[data.name]>.keyframes.elements.message.<[tick]>.message_list:->:<[ran_uuid]>
              #Set new data
              - flag server dcutscenes.<[data.name]>.keyframes.elements.message.<[tick]>.<[ran_uuid]>.message:<[arg_2]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - ~run dcutscene_sort_data def:<[data.name]>
              - define text "Message <white><[arg_2].parse_color> <gray>set at tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>

            #Change message prep
            - case change_message_prep:
              - define text "Chat the message. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_message expire:5m
              - inventory close

            #Change message
            - case change_message:
              - flag <player> cutscene_modify:!
              - define message <[keyframes.message.<[tick]>.<[uuid]>]>
              - define message.message <[arg_2]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.message.<[tick]>.<[uuid]>:<[message]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_keyframe_modify_message
              - define text "Message on tick <green><[tick]>t <gray>is now <white><[arg_2].parse_color> <gray>for scene <green><[data.name]>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Move to prep
            - case move_to_prep:
              - define message_data <[keyframes.message.<[tick]>.<[uuid]>]>
              - definemap move_data animator:message type:move tick:<[tick]> uuid:<[uuid]> data:<[message_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this message animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to
            - case move_to:
              - define move_data <player.flag[dcutscene_animator_change]>
              - define message <[keyframes.message]>
              #Update previous tick
              - define message.<[move_data.tick]>.message_list:<-:<[move_data.uuid]>
              - define message.<[move_data.tick]> <[message.<[move_data.tick]>].deep_exclude[<[move_data.uuid]>]>
              #Check if previous tick is empty
              - if <[message.<[move_data.tick]>.message_list].is_empty>:
                - define message <[message].deep_exclude[<[move_data.tick]>]>
              #Set to new tick
              - define message.<[tick]>.<[move_data.uuid]>:<[move_data.data]>
              #Input to list
              - define message.<[tick]>.message_list:->:<[move_data.uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.message:<[message]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_prep:
              - define message_data <[keyframes.message.<[tick]>.<[uuid]>]>
              - definemap dup_data animator:message type:duplicate tick:<[tick]> uuid:<[uuid]> data:<[message_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this message animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate:
              - define dup_data <player.flag[dcutscene_animator_change]>
              - define message <[keyframes.message]>
              #New uuid
              - define uuid <util.random_uuid>
              #Set to new tick
              - define message.<[tick]>.<[uuid]>:<[dup_data.data]>
              - define message.<[tick]>.message_list:->:<[uuid]>
              #Set updated information
              - flag server dcutscenes.<[data.name]>.keyframes.elements.message:<[message]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - flag <player> dcutscene_animator_change:!
              - ~run dcutscene_sort_data def.cutscene:<[data.name]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Animator <green><[dup_data.animator].replace[_].with[ ]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from this tick
            - case play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove message
            - case remove_message:
              - flag <player> dcutscene_animator_change:!
              - define message <[keyframes.message]>
              #Remove uuid from list
              - define message.<[tick]>.message_list:<-:<[uuid]>
              #Remove the uuid
              - define message.<[tick]> <[message.<[tick]>].deep_exclude[<[uuid]>]>
              #If list is empty remove tick
              - if <[message.<[tick]>.message_list].is_empty>:
                - define message <[message].deep_exclude[<[tick]>]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.message:<[message]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Message has been removed from tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

        #========== Time ===========
        - case time:
          - choose <[arg]>:
            #New time prep
            - case new_time_prep:
              - if <[keyframes.time.<[tick]>]||null> == null:
                - define text "Chat a duration for the time. To stop chat <red>cancel<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - flag <player> cutscene_modify:new_time expire:3m
                - inventory close
              - else:
                - define text "There is already a time set at <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #New time
            - case new_time:
              - if <duration[<[arg_2]>]||null> != null:
                - definemap time_data time:<[arg_2]> duration:<duration[60s]> freeze:false reset:false
                - flag server dcutscenes.<[data.name]>.keyframes.elements.time.<[tick]>:<[time_data]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - ~run dcutscene_sort_data def:<[data.name]>
                - define text "Time <green><duration[<[arg_2]>].in_ticks>t <gray>set at tick <green><[tick]>t <gray>in scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid duration."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change time prep
            - case change_time_prep:
              - define text "Chat a duration for the time. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_time expire:3m
              - inventory close

            #Change time
            - case change_time:
              - if <duration[<[arg_2]>]||null> != null:
                - define time <[keyframes.time.<[tick]>]>
                - define time.time <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.time.<[tick]>:<[time]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_keyframe_modify_time
                - define text "Time on tick <green><[tick]>t <gray>is now <green><duration[<[arg_2]>].in_ticks>t <gray>for scene <green><[data.name]>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid duration."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change time duration
            - case change_time_duration_prep:
              - define text "Chat the duration the time will appear to the player. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_time_duration expire:3m
              - inventory close

            #Change time
            - case change_time_duration:
              - if <duration[<[arg_2]>]||null> != null:
                - define time <[keyframes.time.<[tick]>]>
                - define time.duration <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.time.<[tick]>:<[time]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_keyframe_modify_time
                - define text "Time on tick <green><[tick]>t <gray>duration is now <green><duration[<[arg_2]>].formatted> <gray>for scene <green><[data.name]>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid duration."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Change time freeze
            - case change_time_freeze:
              - define time <[keyframes.time.<[tick]>]>
              - choose <[time.freeze]>:
                - case true:
                  - define time.freeze false
                - case false:
                  - define time.freeze true
              - flag server dcutscenes.<[data.name]>.keyframes.elements.time.<[tick]>:<[time]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - define l1 "<aqua>Freeze: <gray><[time.freeze]>"
              - define l2 "<gray><italic>Click to set freeze time"
              - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
              - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

            #Change time reset
            - case change_time_reset:
              - define time <[keyframes.time.<[tick]>]>
              - choose <[time.reset]>:
                - case true:
                  - define time.reset false
                - case false:
                  - define time.reset true
              - flag server dcutscenes.<[data.name]>.keyframes.elements.time.<[tick]>:<[time]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - define l1 "<gold>Reset Time: <gray><[time.reset]>"
              - define l2 "<gray><italic>Click to set freeze time"
              - define lore <list[<empty>|<[l1]>|<empty>|<[l2]>]>
              - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

            #Move to keyframe prep
            - case move_to_prep:
              - define time_data <[keyframes.time.<[tick]>]>
              - definemap move_data animator:time type:move tick:<[tick]> data:<[time_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this time animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to new keyframe
            - case move_to:
              - if <[keyframes.time.<[tick]>]||null> == null:
                - define move_data <player.flag[dcutscene_animator_change]>
                #Remove previous tick
                - define time <[keyframes.time].deep_exclude[<[move_data.tick]>]>
                #Set previous screeneffect to new tick
                - define time.<[tick]> <[move_data.data]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.time:<[time]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "There is already a time animator at tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_prep:
              - define time_data <[keyframes.time.<[tick]>]>
              - definemap dup_data animator:time type:duplicate tick:<[tick]> data:<[time_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this time animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate:
              - if <[keyframes.time.<[tick]>]||null> == null:
                - define dup_data <player.flag[dcutscene_animator_change]>
                - define time <[keyframes.time]>
                - define time.<[tick]> <[dup_data.data]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.time:<[time]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[dup_data.animator]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "There is already a time animator at tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from this tick
            - case play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove time
            - case remove_time:
              - flag <player> dcutscene_animator_change:!
              - define time <[keyframes.time].deep_exclude[<[tick]>]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.time:<[time]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Time on tick <green><[tick]>t <gray>has been removed from scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

        #========== Weather ===========
        - case weather:
          - choose <[arg]>:
            #New weather
            - case new_weather:
              #Weather check
              - if <[keyframes.weather.<[tick]>]||null> != null:
                - define text "There is already weather on tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
                - stop
              - definemap weather_data weather:sunny duration:1m
              - flag server dcutscenes.<[data.name]>.keyframes.elements.weather.<[tick]>:<[weather_data]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - ~run dcutscene_sort_data def:<[data.name]>
              - define text "Weather set on tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - inventory open d:dcutscene_inventory_keyframe_modify_weather

            #Change weather
            - case change_weather:
              - define weather <[keyframes.weather.<[tick]>]>
              - choose <[weather.weather]>:
                - case sunny:
                  - define weather.weather storm
                - case storm:
                  - define weather.weather thunder
                - case thunder:
                  - define weather.weather reset
                - case reset:
                  - define weather.weather sunny
              - flag server dcutscenes.<[data.name]>.keyframes.elements.weather.<[tick]>:<[weather]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - define l1 "<white>Weather: <gray><[weather.weather]>"
              - define modify "<gray><italic>Click to modify weather"
              - define lore <list[<empty>|<[l1]>|<empty>|<[modify]>]>
              - inventory adjust d:<player.open_inventory> slot:<[arg_2]> lore:<[lore]>

            #Change weather duration prep
            - case change_weather_duration_prep:
              - define text "Chat the duration of how long the weather will appear for the player. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - flag <player> cutscene_modify:change_weather_duration expire:3m
              - inventory close

            #Change weather duration
            - case change_weather_duration:
              - if <duration[<[arg_2]>]||null> != null:
                - define weather <[keyframes.weather.<[tick]>]>
                - define weather.duration <[arg_2]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.weather.<[tick]>:<[weather]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - inventory open d:dcutscene_inventory_keyframe_modify_weather
                - define text "Weather on tick <green><[tick]>t <gray>will now appear for <green><duration[<[arg_2]>].formatted> <gray>in scene <green><[data.name]>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "<green><[arg_2]> <gray>is not a valid duration."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Move to keyframe prep
            - case move_to_prep:
              - define weather_data <[keyframes.weather.<[tick]>]>
              - definemap move_data animator:weather type:move tick:<[tick]> data:<[weather_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[move_data]> expire:3m
              - define text "Click on the tick you'd like to move this weather animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Move to new keyframe
            - case move_to:
              - if <[keyframes.weather.<[tick]>]||null> == null:
                - define move_data <player.flag[dcutscene_animator_change]>
                #Remove previous tick
                - define weather <[keyframes.weather].deep_exclude[<[move_data.tick]>]>
                #Set previous screeneffect to new tick
                - define weather.<[tick]> <[move_data.data]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.weather:<[weather]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[move_data.animator]> <gray>from tick <green><[move_data.tick]>t <gray>has been moved to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "There is already a weather animator at tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Duplicate prep
            - case duplicate_prep:
              - define weather_data <[keyframes.weather.<[tick]>]>
              - definemap dup_data animator:weather type:duplicate tick:<[tick]> data:<[weather_data]> scene:<[data.name]>
              - flag <player> dcutscene_animator_change:<[dup_data]> expire:3m
              - define text "Click on the tick you'd like to duplicate this weather animator to. To stop chat <red>cancel<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"
              - ~run dcutscene_keyframe_modify def:back

            #Duplicate
            - case duplicate:
              - if <[keyframes.weather.<[tick]>]||null> == null:
                - define dup_data <player.flag[dcutscene_animator_change]>
                - define weather <[keyframes.weather]>
                - define weather.<[tick]> <[dup_data.data]>
                - flag server dcutscenes.<[data.name]>.keyframes.elements.weather:<[weather]>
                - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
                - flag <player> dcutscene_animator_change:!
                #Sort the data
                - ~run dcutscene_sort_data def.cutscene:<[data.name]>
                - inventory open d:dcutscene_inventory_sub_keyframe
                - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
                - define text "Animator <green><[dup_data.animator]> <gray>from tick <green><[dup_data.tick]>t <gray>has been duplicated to tick <green><[tick]>t <gray>for scene <green><[data.name]><gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"
              - else:
                - define text "There is already a weather animator at tick <green><[tick]>t<gray>."
                - narrate "<[msg_prefix]> <gray><[text]>"

            #Play from this tick
            - case play_from_here:
              - inventory close
              - run dcutscene_animation_begin def.scene:<[data.name]> def.player:<player> def.timespot:<[tick].sub[1]>t
              - define text "Playing scene <green><[data.name]> <gray>on tick <green><[tick]>t<gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"

            #Remove weather
            - case remove_weather:
              - flag <player> dcutscene_animator_change:!
              - define weather <[keyframes.weather].deep_exclude[<[tick]>]>
              - flag server dcutscenes.<[data.name]>.keyframes.elements.weather:<[weather]>
              - flag <player> cutscene_data:<server.flag[dcutscenes.<[data.name]>]>
              - inventory open d:dcutscene_inventory_sub_keyframe
              - ~run dcutscene_sub_keyframe_modify def:<player.flag[dcutscene_sub_keyframe_back_data]>
              - define text "Weather on tick <green><[tick]>t <gray>has been removed from scene <green><[data.name]><gray>."
              - narrate "<[msg_prefix]> <gray><[text]>"