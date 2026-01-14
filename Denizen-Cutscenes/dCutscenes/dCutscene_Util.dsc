# Denizen Cutscenes Utility
# Utility tasks and procedures for Denizen Cutscenes

#=========== Cutscene Tasks =============

#Create new cutscene from scratch
dcutscene_new_scene:
    type: task
    debug: false
    definitions: type|scene
    script:
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    # New cutscene name
    - if !<[type].exists>:
      - flag <player> cutscene_modify:new_name expire:60s
      - define text "Chat the name of the new cutscene. Chat <red>cancel <gray>to stop."
      - narrate "<[msg_prefix]> <gray><[text]>"
    - else:
      - if !<[scene].exists>:
        - debug error "No scene specified for dcutscene_new_scene"
        - stop
      - if <server.flag[dcutscenes.<[scene]>]||null> != null:
        - define text "A cutscene with the name <green><underline><[scene]> <gray>already exists!"
        - narrate "<[msg_prefix]> <gray><[text]>"
        - stop
      - flag <player> cutscene_modify:!
      # Default data creation
      - define cutscene.name <[scene].parse_color.strip_color>
      - define cutscene.display <[scene]>
      - define l1 "&bA specially crafted cutscene"
      - define l2 "&bjust for you <player.name>."
      - define cutscene.description <list[<[l1]>|<[l2]>]>
      - define cutscene.world <list[<player.world.name>]>
      - define cutscene.length 0
      - define cutscene.keyframes <empty>
      - definemap settings_data bars:true item:<item[dcutscene_scene_item_default]> hide_players:false camera_bound:true origin:false
      - define cutscene.settings <[settings_data]>
      - flag server dcutscenes.<[cutscene.name]>:<[cutscene]>
      - ~run dcutscene_scene_show def:back
      - define text "New cutscene <green><[cutscene.name]> <gray>has been created."
      - narrate "<[msg_prefix]> <gray><[text]>"

# Removes the scene from the server flag dcutscenes
dcutscene_remove_scene:
    type: task
    debug: false
    definitions: cutscene
    script:
    - if <server.flag[dcutscenes.<[cutscene]>]||null> == null:
      - debug error "Invalid cutscene specified in dcutscene_remove_scene"
    - else:
      - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
      - flag server dcutscenes:<server.flag[dcutscenes].deep_exclude[<[cutscene]>]>
      - run dcutscene_scene_show
      - define text "Scene <green><[cutscene]> <gray>has been deleted."
      - narrate "<[msg_prefix]> <gray><[text]>"

# Change the name of the cutscene
dcutscene_settings_modify:
    type: task
    debug: false
    definitions: option|arg
    script:
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - define data <server.flag[dcutscenes]>
    - define cutscene <player.flag[cutscene_data]>
    - choose <[option]>:
      #Change cutscene name prep
      - case new_name_prep:
        - define text "Chat the new name for this cutscene."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - flag <player> cutscene_modify:cutscene_new_name expire:2m
        - inventory close
      #Change cutscene name
      - case change_name:
        - define validate <[data.<[arg]>]||null>
        - if <[validate]> != null:
          - define text "There is already a cutscene named <green><[arg]><gray>."
          - narrate "<[msg_prefix]> <gray><[text]>"
          - stop
        - else:
          - define cutscene.name <[arg].parse_color.strip_color>
          - define cutscene.display <[arg]>
          - flag server dcutscenes.<[cutscene.name]>:<[cutscene]>
          - flag <player> cutscene_data:<server.flag[dcutscenes.<[arg]>]>
          - define text "Scene <green><[cutscene.name]> <gray>has been renamed to <green><[cutscene.name]>."
          - narrate "<[msg_prefix]> <gray><[text]>"
          - ~run dcutscene_scene_show
          - flag <player> cutscene_modify:!

      #Change cutscene description prep
      - case change_desc_prep:
        - define text "Chat the description for this cutscene. To go to the next line use a semicolon '<green>;<gray>'."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - flag <player> cutscene_modify:cutscene_new_desc expire:10m
        - inventory close
      #Change cutscene description
      - case change_desc:
        - define cutscene.description <[arg].split[;]>
        - flag server dcutscenes.<[cutscene.name]>:<[cutscene]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[cutscene.name]>]>
        - define text "Cutscene <green><[cutscene.name]> <gray>description has been modified."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - flag <player> cutscene_modify:!
        - ~run dcutscene_scene_show

      #Determine whether cutscene bars will shown or not
      - case change_bars:
        - choose <[cutscene.settings.bars].if_null[true]>:
          - case true:
            - define cutscene.settings.bars false
            - define bool false
          - case false:
            - define cutscene.settings.bars true
            - define bool true
        - flag server dcutscenes.<[cutscene.name]>:<[cutscene]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[cutscene.name]>]>
        - define l1 "<blue>Cutscene Bars: <gray><[bool]>"
        - define click "<gray><italic>Click to change cutscene bars"
        - define lore <list[<empty>|<[l1]>|<empty>|<[click]>]>
        - inventory adjust d:<player.open_inventory> slot:<[arg]> lore:<[lore]>

      #Preparation for duplicate cutscene
      - case duplicate_prep:
        - define text "Chat a new name for the duplicate cutscene. Chat <red>cancel <gray>to stop."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - flag <player> cutscene_modify:cutscene_duplicate expire:2m
        - inventory close

      #Duplicate the cutscene
      - case duplicate:
        - define scene_check <[data.<[arg]>]||null>
        - if <[scene_check]> != null:
          - define text "There is already a cutscene named <green><[arg]><gray>."
          - stop
        - define dup_scene <[cutscene]>
        - define dup_scene.name <[arg].parse_color.strip_color>
        - define dup_scene.display <[arg]>
        - flag server dcutscenes.<[dup_scene.name]>:<[dup_scene]>
        - define text "Cutscene <green><[cutscene.name]> <gray>has been duplicated to <green><[dup_scene.name]>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - ~run dcutscene_scene_show

      #Change cutscene gui item prep
      - case change_item_prep:
        - define text "Chat a valid item tag or chat <green>hand <gray>for the item in your hand."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - flag <player> cutscene_modify:cutscene_new_gui_item expire:2m
        - inventory close
      #Change cutscene gui item
      - case change_item:
        - if <item[<[arg]>]||null> == null && <item[<[arg]>].material.name> == air:
          - define text "<green><[arg]> <gray>is not a valid item."
          - narrate "<[msg_prefix]> <gray><[text]>"
          - stop
        - adjust <item[<[arg]>]> quantity:1 save:item
        - define item <entry[item].result>
        - define cutscene.settings.item <[arg]>
        - flag server dcutscenes.<[cutscene.name]>:<[cutscene]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[cutscene.name]>]>
        - define text "Cutscene <green><[cutscene.name].parse_color> <gray>GUI item is now <green><[arg]><gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - ~run dcutscene_scene_show
        - flag <player> cutscene_modify:!

      #Determine if players are hidden or not during the cutscene
      - case change_hide_players:
        - choose <[cutscene.settings.hide_players].if_null[false]>:
          - case true:
            - define cutscene.settings.hide_players false
            - define bool false
          - case false:
            - define cutscene.settings.hide_players true
            - define bool true
        - flag server dcutscenes.<[cutscene.name]>:<[cutscene]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[cutscene.name]>]>
        - define l1 "<gold>Hide players: <gray><[bool]>"
        - define click "<gray><italic>Click to change hide players"
        - define lore <list[<empty>|<[l1]>|<empty>|<[click]>]>
        - inventory adjust d:<player.open_inventory> slot:<[arg]> lore:<[lore]>

      #Determine if the player is bound to the camera
      - case change_camera_bound:
        - choose <[cutscene.settings.camera_bound].if_null[true]>:
          - case true:
            - define cutscene.settings.camera_bound false
            - define bool false
          - case false:
            - define cutscene.settings.camera_bound true
            - define bool true
        - flag server dcutscenes.<[cutscene.name]>:<[cutscene]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[cutscene.name]>]>
        - define l1 "<dark_gray>Bound to camera: <gray><[bool]>"
        - define click "<gray><italic>Click to change bound to camera"
        - define lore <list[<empty>|<[l1]>|<empty>|<[click]>]>
        - inventory adjust d:<player.open_inventory> slot:<[arg]> lore:<[lore]>

      #Remove cutscene prep
      - case remove_scene_prep:
        - clickable dcutscene_settings_modify def:remove_scene|<[cutscene.name]> usages:1 save:remove_scene
        - define prefix <[msg_prefix]>
        - define text "Are you sure you want to remove <green><[cutscene.name]><gray>? It is suggested you make a backup using the save button before proceeding. <green><bold><element[Yes].on_hover[<[prefix]> <gray>This will remove this cutscene from the server.].type[SHOW_TEXT].on_click[<entry[remove_scene].command>]>"
        - narrate "<[prefix]> <gray><[text]>"
        - inventory close
      #Remove cutscene
      - case remove_scene:
        - define scene <[arg]>
        - define new_data <[data].deep_exclude[<[scene]>]>
        - if <[new_data].keys.size||0> == 0:
          - flag server dcutscenes:!
        - else:
          - flag server dcutscenes:<[new_data]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[arg]>]>
        - define text "Scene <green><[scene].parse_color> <gray>has been removed from the server."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - ~run dcutscene_scene_show

      #Set cutscene origin point prep
      - case set_origin_point_prep:
        - define text "Chat a valid location tag or right click on a block to set the cutscene origin point. Chat <red>false <gray>to disable."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - flag <player> cutscene_modify:set_origin_point
        - inventory close
      #Set cutscene origin point
      - case set_origin_point:
        - flag <player> cutscene_modify:!
        - define loc <location[<[arg].parsed>]||null>
        - if <[loc]> == null:
          - define text "<green><[arg]> <gray>is not a valid location."
          - narrate "<[msg_prefix]> <gray><[text]>"
          - stop
        - define cutscene.settings.origin:<[loc]>
        - flag server dcutscenes.<[cutscene.name]>:<[cutscene]>
        - flag <player> cutscene_data:<server.flag[dcutscenes.<[cutscene.name]>]>
        - define text "Cutscene <green><[cutscene.name].parse_color> <gray>origin point is now <green><[arg]><gray>."
        - narrate "<[msg_prefix]> <gray><[text]>"
        - inventory open d:dcutscene_inventory_settings
        - ~run dcutscene_origin_set def.scene:<[cutscene.name]> def.origin:<[loc]>

#====== Cutscene Origin Point ======#
# Calculates relative offsets based on the origin point
dcutscene_origin_set:
    type: task
    definitions: scene|origin
    debug: false
    script:
    - define data <server.flag[dcutscenes.<[scene]>]||null>
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - if <[data]> == null:
      - define text "Cutscene <green><[scene].parse_color> <gray>does not exist."
      - debug error "<[msg_prefix]> <gray><[text]>"
      - determine false
    - define keyframes <[data.keyframes]||>
    - if !<[keyframes].is_truthy>:
      - determine false
    - if !<[origin].exists>:
      - define origin <location[<[data.settings.origin]>]||false>
      - if !<[origin].is_truthy>:
        - determine false
    - foreach <[keyframes]> key:animator_name as:keyframe:
      - choose <[animator_name]>:
        - case camera:
          - foreach <[keyframes.camera]> key:tick as:cam_data:
            - define loc <[cam_data.location]||null>
            - define offset <[origin].proc[dcutscene_offset].context[<[loc].as[location]>]>
            - define keyframes.camera.<[tick]>.origin_offset:<[offset]>
            - define loc_count:++
        - case elements:
          - foreach <[keyframes.elements].keys> as:element:
            # Fake object
            - if <[element]> == fake_object:
              - foreach <[keyframes.elements.fake_object].keys> as:fake_object:
                - define fake_object_data <[keyframes.elements.fake_object.<[fake_object]>]>
                - foreach <[fake_object_data]> key:tick as:fake_object_tick:
                  - if <[fake_object_tick.fake_blocks].exists>:
                    - define list <[fake_object_tick.fake_blocks]>
                  - else if <[fake_object_tick.fake_schems].exists>:
                    - define list <[fake_object_tick.fake_schems]>
                  - foreach <[list]||<list>> as:fake_object_uuid:
                    - define loc <[fake_object_data.<[tick]>.<[fake_object_uuid]>.loc]||null>
                    - if <[loc]> == null:
                      - debug error "Could not determine location for animator <[fake_object]> in tick <[tick]> for scene <[scene]>"
                      - foreach next
                    - define offset <[origin].proc[dcutscene_offset].context[<[loc].as[location]>]>
                    - define keyframes.elements.fake_object.<[fake_object]>.<[tick]>.<[fake_object_uuid]>.origin_offset:<[offset]>
                    - define loc_count:++
            - else if <[element]> in particle|sound:
              - foreach <[keyframes.elements.<[element]>]> key:tick as:element_data:
                - if <[element_data.particle_list].exists>:
                  - define list <[element_data.particle_list]>
                - else if <[element_data.sounds].exists>:
                  - define list <[element_data.sounds]>
                - foreach <[list]> as:element_uuid:
                  - define loc <[element_data.<[element_uuid]>.loc]||<[element_data.<[element_uuid]>.location].if_null[null]>>
                  - if <[loc]> == null:
                    - debug error "Could not determine location for animator <[element]> in tick <[tick]> for uuid <[element_uuid]> in scene <[scene]>"
                    - foreach next
                  - define offset <[origin].proc[dcutscene_offset].context[<[loc].as[location]>]>
                  - define keyframes.elements.<[element]>.<[tick]>.<[element_uuid]>.origin_offset:<[offset]>
                  - define loc_count:++
    - flag server dcutscenes.<[scene]>.keyframes:<[keyframes]>
    - determine <[loc_count]||0>

# Returns a vector to be used with the .add tag when an origin point is set
dcutscene_offset:
    type: procedure
    debug: false
    definitions: loc|loc2
    script:
    - determine <[loc2].x.sub[<[loc].x>]>,<[loc2].y.sub[<[loc].y>]>,<[loc2].z.sub[<[loc].z>]||0,0,0>

#======= Cutscene Command ========
dcutscene_command:
    type: command
    name: dcutscene
    usage: /dcutscene
    aliases:
    - dscene
    description: Cutscene command for DCutscene
    tab completions:
      1: <list[load|save|play|location|loc|animate|model|sound|material|particle|stop|item]>
      2: <player.proc[dcutscene_data_list].if_null[<empty>]>
    permission: dcutscene.op
    script:
    - define a_1 <context.args.get[1]||null>
    - define a_2 <context.args.get[2]||null>
    - define a_3 <context.args.get[3]||null>
    - if <[a_1]> == null:
      - ~run dcutscene_scene_show
    - else:
      - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
      - choose <[a_1]>:
        #Item for opening the gui
        - case item:
          - give dcutscene_open_gui_item
        #Open the location tool GUI
        - case location:
          - if <player.has_flag[cutscene_modify]>:
            - inventory clear
            - inventory open d:dcutscene_inventory_location_tool
          - else:
            - define text "You do not have an object selected to modify it's location."
            - narrate "<[msg_prefix]> <gray><[text]>"
        #Same thing as above
        - case loc:
          - if <player.has_flag[cutscene_modify]>:
            - inventory clear
            - inventory open d:dcutscene_inventory_location_tool
          - else:
            - define text "You do not have an object selected to modify it's location."
            - narrate "<[msg_prefix]> <gray><[text]>"
        #Load dcutscene files
        - case load:
          - if <[a_2]> == null:
            - ~run dcutscene_load_file def:all
          - else:
            - ~run dcutscene_load_file def:<[a_2]>
        #Save dcutscene files to a directory
        - case save:
          - if <[a_2]> != null && <server.flag[dcutscenes.<[a_2]>]||null> != null:
            - ~run dcutscene_save_file def.cutscene:<[a_2]>
            - narrate "<[msg_prefix]> <gray>Saved cutscene <green><[a_2]> <gray>to <green>"Denizen/data/dcutscenes/scenes/<[a_2]>.dcutscene<gray>.""
          - else:
            - narrate "<[msg_prefix]> <gray>That cutscene does not seem to exist."
        #Play a cutscene
        - case play:
          - if <[a_2]> != null:
            - if <server.flag[dcutscenes.<[a_2]>]||null> != null:
              - if <[a_3]> == null:
                - run dcutscene_animation_begin def:<[a_2]>
              - else if <[a_3]> != null:
                - if <player[<[a_3]>]||null> != null:
                  - run dcutscene_animation_begin def.scene:<[a_2]> def.player:<player[<[a_3]>]>
                - else:
                  - define text "<green><[a_3]> <gray>is not a valid player."
                  - narrate "<[msg_prefix]> <gray><[text]>"
            - else:
              - define text "Cutscene <red><[a_2]> <gray>does not exist!"
              - narrate "<[msg_prefix]> <gray><[text]>"
        #Stop a cutscene from playing
        - case stop:
          - if <[a_2]> == null:
            - run dcutscene_animation_stop def:<player>
          - else if <player[<[a_2]>]||null> != null:
            - run dcutscene_animation_stop def:<player[<[a_2]>]>
        #Animate the model the player is modifying.
        - case animate:
          - if <player.has_flag[cutscene_modify]>:
            - if <[a_2]> != null:
              #Animation for fake spawned model the creator is modifying
              - if <player.flag[cutscene_modify]> != set_model_animation:
                - define data <player.flag[dcutscene_location_editor]||null>
                - if <[data]> != null:
                  - define root <[data.root_ent]||null>
                  - if <[root]> == null:
                    - define text "Could not find model to animate"
                    - stop
                  - define type <[data.root_type]||null>
                  - if <[type]> != null:
                    - choose <[type]>:
                      - case player_model:
                        - run pmodels_animate def:<[root]>|<[a_2]>
                      - case model:
                        - run dmodels_animate def:<[root]>|<[a_2]>
              #Set animation for model in keyframe
              - else if <player.flag[cutscene_modify]> == set_model_animation:
                - define type <player.flag[dcutscene_save_data.type]>
                - choose <[type]>:
                  - case player_model:
                    #Validate the animation
                    - if <server.flag[pmodels_data.animations_player_model_template_norm.<[a_2]>]||null> == null && <[a_2]> != false && <[a_2]> != stop:
                      - define text "Animation <green><[a_2]> <gray>does not seem to exist."
                      - narrate "<[msg_prefix]> <gray><[text]>"
                      - stop
                    - run dcutscene_model_keyframe_edit def:player_model|animate|set_animation|<[a_2]>
                  - case model:
                    #Validate the animation
                    - define model <player.flag[dcutscene_save_data.model]>
                    - if <server.flag[dmodels_data.animations_<[model]>.<[a_2]>]||null> == null && <[a_2]> != false && <[a_2]> != stop:
                      - define text "Animation <green><[a_2]> <gray>does not seem to exist for model <green><[model]><gray>."
                      - narrate "<[msg_prefix]> <gray><[text]>"
                      - stop
                    - run dcutscene_model_keyframe_edit def:denizen_model|set_animation|<[a_2]>
        #Shows list of models
        - case model:
          - if <player.has_flag[cutscene_modify]>:
            - if <[a_2]> != null && <player.flag[cutscene_modify]> == set_model_name:
              - run dcutscene_model_keyframe_edit def:denizen_model|create_model_name|<[a_2]>
            - else if <[a_2]> != null && <player.flag[cutscene_modify]> == change_model_prep:
              - run dcutscene_model_keyframe_edit def:denizen_model|change_model|<[a_2]>
        #Input a new sound
        - case sound:
          - if <player.has_flag[cutscene_modify]>:
            - if <[a_2]> != null && <player.flag[cutscene_modify]> == sound:
              - run dcutscene_animator_keyframe_edit def:sound|create|<[a_2]>
        #Set new material
        - case material:
          - if <player.has_flag[cutscene_modify]>:
            - if <[a_2]> != null:
              - if <player.flag[cutscene_modify]> == fake_block_material:
                - run dcutscene_animator_keyframe_edit def:fake_object|new_fake_block_material_set|<[a_2]>
              - else if <player.flag[cutscene_modify]> == set_fake_block_material:
                - run dcutscene_animator_keyframe_edit def:fake_object|set_fake_block_material|<[a_2]>
        #Set new particle
        - case particle:
          - if <player.has_flag[cutscene_modify]>:
            - if <[a_2]> != null:
              - if <player.flag[cutscene_modify]> == new_particle:
                - run dcutscene_animator_keyframe_edit def:particle|new_particle_loc|<[a_2]>
              - else if <player.flag[cutscene_modify]> == change_particle:
                - run dcutscene_animator_keyframe_edit def:particle|change_particle|<[a_2]>

# Tab completion for list of cutscenes or animator modifiers that utilize data from the server
dcutscene_data_list:
    type: procedure
    debug: false
    definitions: player
    script:
    - if !<[player].has_flag[cutscene_modify]> && !<[player].has_flag[cutscene_modify_tab]>:
      - determine <server.flag[dcutscenes].keys||<empty>>
    - choose <[player].flag[cutscene_modify_tab]>:
      - case model:
        - determine <server.flag[dmodels_data].keys.filter[starts_with[model_]].parse[after[model_]].if_null[<empty>]>
      - case sound:
        - determine <server.sound_types>
      - case animate:
        - if <[player].flag[cutscene_modify]> == set_model_animation:
          - define type <[player].flag[dcutscene_save_data.type]>
        - else:
          - define type <[player].flag[dcutscene_location_editor.root_type]||null>
        - if <[type]> == null:
          - determine <empty>
        - else:
          - choose <[type]>:
            - case player_model:
              - define anim_list <server.flag[pmodels_data.animations_player_model_template_norm]||<map>>
              - determine <[anim_list].keys||<empty>>
            - case model:
              - define model <[player].flag[dcutscene_save_data.model]>
              - define anim_list <server.flag[dmodels_data.animations_<[model]>]||<map>>
              - determine <[anim_list].keys||<empty>>
      - case material:
        - determine <server.material_types.filter[is_block].parse_tag[<material[<[parse_value]>].name>]>
      - case particle:
        - determine <server.particle_types>
      - default:
        - determine <empty>
    - determine <empty>

## Data Operations #########
# Saves a cutscene to a directory
dcutscene_save_file:
    type: task
    debug: false
    definitions: cutscene
    script:
    - if <server.flag[dcutscenes.<[cutscene.name]>]||null> == null:
      - debug error "Could not find cutscene to save."
    - else:
      - if <script[dcutscenes_config].data_key[config].get[cutscene_compress_save_file].if_null[false].is_truthy>:
        - define indent 0
      - else:
        - define indent 4
      - define cutscene_data <server.flag[dcutscenes.<[cutscene.name]>]>
      - define c_id <[cutscene_data.name]>
      - ~filewrite path:data/dcutscenes/scenes/<[c_id]>.dcutscene data:<[cutscene].to_json[native_types=true;indent=<[indent]>].utf8_encode>

# Data debugger to visualize the data in "Denizen/data/dcutscenes/debug"
dcutscene_debugger:
    type: task
    debug: false
    definitions: file|data
    script:
    - define msg_prefix <script[dcutscenes_config].data_key[config].get[cutscene_prefix].parse_color||<&color[0,0,255]><bold>DCutscenes>
    - if <[file]> == null:
      - debug error "No file specified."
      - stop
    - if <[data]> == null:
      - debug error "No data specified."
      - stop
    - ~filewrite path:data/dcutscenes/debug/<[file]>.json data:<[data].to_json[native_types=true;indent=4].utf8_encode>
    - define text "Debug information sent to <green>data/dcutscenes/debug/<[file]>.json<gray>."
    - narrate "<[msg_prefix]> <gray><[text]>"

# Cutscene file load validation
dcutscene_load_file:
    type: task
    debug: false
    definitions: file
    script:
    - if !<[file].exists>:
      - debug error "No file specified."
      - stop
    - if <[file]> == all:
      - foreach <util.list_files[data/dcutscenes/scenes]||null> as:file:
        - if !<[file].split[.].contains[dcutscene]>:
          - debug error "<[file]> is not a dcutscene file in Denizen/data/dcutscenes/scenes"
        - else:
          - ~run dcutscene_load_file_data def.file:<[file]>
    - else:
      - if <util.list_files[data/dcutscenes/scenes].contains[<[file]>.dcutscene]||<list>.any>:
        - debug error "File <[file]> could not be found in dcutscene_load_file"
        - stop
      - run dcutscene_load_file_data def.file:<[file]>.dcutscene

# Loads cutscene file
dcutscene_load_file_data:
    type: task
    debug: false
    definitions: file
    script:
    - ~yaml id:file_<[file]> load:data/dcutscenes/scenes/<[file]>
    - define name <yaml[file_<[file]>].read[name]||null>
    - if <[name]> == null:
      - debug error "<[file]> is an invalid dcutscene file"
      - foreach next
    - define cutscene.name <[name]>
    - define cutscene.display <yaml[file_<[file]>].read[display]||<[name]>>
    - define cutscene.name_color <yaml[file_<[file]>].read[name_color]||<empty>>
    - define cutscene.description <yaml[file_<[file]>].read[description]||<empty>>
    - define cutscene.world <yaml[file_<[file]>].read[world]||<empty>>
    - define cutscene.item <yaml[file_<[file]>].read[item]||<empty>>
    - define cutscene.length <yaml[file_<[file]>].read[length]||<empty>>
    - define cutscene.keyframes <yaml[file_<[file]>].read[keyframes]||<empty>>
    - define cutscene.settings <yaml[file_<[file]>].read[settings]||<empty>>
    - flag server dcutscenes.<[name]>:<[cutscene]>
    - ~run dcutscene_sort_data def:<[cutscene.name]>
    - announce to_console "[Denizen Cutscenes] Cutscene <[name]> has been loaded."
    - yaml unload id:file_<[file]>

# Sort the keyframes
dcutscene_sort_data:
    type: task
    debug: false
    definitions: cutscene
    script:
    - if !<[cutscene].exists>:
      - debug error "DCutscenes There is no cutscenes to sort!"
    - else:
      - define data <server.flag[dcutscenes.<[cutscene]>]||null>
      - if <[data]> == null:
        - debug error "Invalid cutscene <[cutscene]> in dcutscene_sort_data."
        - stop
      - define name <[data.name]>
      - define keyframes <[data.keyframes]>
      #Camera
      - define camera <[keyframes.camera].sort_by_value[get[tick]]||null>
      - if <[camera]> != null:
        - define keyframes.camera <[camera]>
      #Models
      - define models <[keyframes.models]||null>
      - if <[models]> != null:
        #Models by tick
        - foreach <[models]> key:tick as:model:
          - define model_list <[model.model_list]>
          #Model List
          - foreach <[model_list]> as:m_uuid:
            #If the model is a root model proceed
            - if <[model.<[m_uuid]>.root]> == none:
              #If there is a path sort it by time and update the data
              - if <[model.<[m_uuid]>.path].is_truthy>:
                - define path <[model.<[m_uuid]>.path].sort_by_value[get[tick]]||null>
                - if <[path]> != null:
                  - define models.<[tick]>.<[m_uuid]>.path <[path]>
        - define keyframes.models <[models]>
      #Run Task
      - define run_task <[keyframes.elements.run_task].sort_by_value[get[tick]]||null>
      - if <[run_task]> != null:
        - define keyframes.elements.run_task <[run_task]>
      #Fake Object
      - define fake_object <[keyframes.elements.fake_object].sort_by_value[get[tick]]||null>
      - if <[fake_object]> != null:
        - define keyframes.elements.fake_object <[fake_object]>
      #Particle
      - define particle <[keyframes.elements.particle].sort_by_value[get[tick]]||null>
      - if <[particle]> != null:
        - define keyframes.elements.particle <[particle]>
      #Screeneffect
      - define screeneffect <[keyframes.elements.screeneffect].sort_by_value[get[tick]]||null>
      - if <[screeneffect]> != null:
        - define keyframes.elements.screeneffect <[screeneffect]>
      #Sound
      - define sound <[keyframes.elements.sound].sort_by_value[get[tick]]||null>
      - if <[sound]> != null:
        - define keyframes.elements.sound <[sound]>
      #Title
      - define title <[keyframes.elements.title].sort_by_value[get[tick]]||null>
      - if <[title]> != null:
        - define keyframes.elements.title <[title]>
      #Command
      - define command <[keyframes.elements.command].sort_by_value[get[tick]]||null>
      - if <[command]> != null:
        - define keyframes.elements.command <[command]>
      #Message
      - define message <[keyframes.elements.message].sort_by_value[get[tick]]||null>
      - if <[message]> != null:
        - define keyframes.elements.message <[message]>
      #Time
      - define time <[keyframes.elements.time].sort_by_value[get[tick]]||null>
      - if <[time]> != null:
        - define keyframes.elements.time <[time]>
      #Weather
      - define weather <[keyframes.elements.weather].sort_by_value[get[tick]]||null>
      - if <[weather]> != null:
        - define keyframes.elements.weather <[weather]>
      #Scene Length
      - define ticks <proc[dcutscene_animation_length].context[<[cutscene]>]>
      #Total cutscene length
      - if <[ticks].any>:
        - define animation_length <duration[<[ticks].highest>t].in_seconds>s
        - define data.length <[animation_length]>
        - flag server dcutscenes.<[name]>.length:<[data.length]>
      - flag server dcutscenes.<[name]>.keyframes:<[keyframes]>

# Returns total animation length of the cutscene
dcutscene_animation_length:
    type: procedure
    debug: false
    definitions: cutscene
    script:
    - define data <server.flag[dcutscenes.<[cutscene]>]>
    - define keyframes <[data.keyframes]>
    #Camera
    - define ticks <list>
    - define cam_keyframes <[keyframes.camera]||null>
    - if <[cam_keyframes]> != null:
      - define highest <[cam_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Model
    - define model_keyframes <[keyframes.models]||null>
    - if <[model_keyframes]> != null:
      - define highest <[model_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Run Task
    - define run_task_keyframes <[keyframes.elements.run_task]||null>
    - if <[run_task_keyframes]> != null:
      - define highest <[run_task_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Fake Blocks
    - define fake_block_keyframes <[keyframes.elements.fake_object.fake_block]||null>
    - if <[fake_block_keyframes]> != null:
      - define highest <[fake_block_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Fake Schems
    - define fake_schem_keyframes <[keyframes.elements.fake_object.fake_schem]||null>
    - if <[fake_schem_keyframes]> != null:
      - define highest <[fake_schem_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Particle
    - define particle_keyframes <[keyframes.elements.particle]||null>
    - if <[particle_keyframes]> != null:
      - define highest <[particle_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Screeneffect
    - define effect_keyframes <[keyframes.elements.screeneffect]||null>
    - if <[effect_keyframes]> != null:
      - define highest <[effect_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Sound
    - define sound_keyframes <[keyframes.elements.sound]||null>
    - if <[sound_keyframes]> != null:
      - define highest <[cam_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Title
    - define title_keyframes <[keyframes.elements.title]||null>
    - if <[title_keyframes]> != null:
      - define highest <[title_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Command
    - define command_keyframes <[keyframes.elements.command]||null>
    - if <[command_keyframes]> != null:
      - define highest <[command_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Message
    - define message_keyframes <[keyframes.elements.message]||null>
    - if <[message_keyframes]> != null:
      - define highest <[message_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Time
    - define time_keyframes <[keyframes.elements.time]||null>
    - if <[time_keyframes]> != null:
      - define highest <[time_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Weather
    - define weather_keyframes <[keyframes.elements.weather]||null>
    - if <[weather_keyframes]> != null:
      - define highest <[weather_keyframes].keys.highest||0>
      - define ticks:->:<[highest]>
    #Play scene
    - define play_scene <[keyframes.play_scene]||null>
    - if <[play_scene]> != null:
      - define ticks:->:<[play_scene.tick]>
    #Stop Point
    - define stop_point <[keyframes.stop]||null>
    - if <[stop_point]> != null:
      - define ticks:->:<[stop_point.tick]>
    - determine <[ticks]>

## Utility Example Procedures ############
#See wiki for example usages you can use these for animators that allow location procedures

#Input 1: LocationTag
#Input 2: The axis of rotation x,y,z
#Input 3: The radius or size of the circle
#Input 4: How many points in the circle
dcutscene_circle_proc:
  type: procedure
  debug: false
  definitions: loc|axis|radius|points
  script:
  - define axis <[axis]||y>
  - define radius <[radius]||10>
  - define points <[points]||10>
  - choose <[axis]>:
    - case x:
      - determine <[loc].points_around_x[radius=<[radius]>;points=<[points]>]>
    - case y:
      - determine <[loc].points_around_y[radius=<[radius]>;points=<[points]>]>
    - case z:
      - determine <[loc].points_around_z[radius=<[radius]>;points=<[points]>]>

#Input 1: LocationTag
#Input 2: Vector offset such as 2,5,1
#Input 3: Another vector
dcutscene_cube_proc:
  type: procedure
  debug: false
  definitions: loc|vec|vec_2
  script:
  - define vec <[vec]||10,10,10>
  - define vec_2 <[vec_2]||null>
  - if <[vec_2]> == null:
    - determine <[loc].to_cuboid[<[loc].add[<[vec]>]>].shell>
  - else:
    - determine <[loc].add[<[vec_2]>].to_cuboid[<[loc].add[<[vec]>]>].shell>

#Input 1: LocationTag
#Input 2: Vector offset such as 2,5,1
#Input 3: Another vector
dcutscene_cube_holo_proc:
  type: procedure
  debug: false
  definitions: loc|vec|vec_2
  script:
  - define vec <[vec]||10,10,10>
  - define vec_2 <[vec_2]||null>
  - if <[vec_2]> == null:
    - determine <[loc].to_cuboid[<[loc].add[<[vec]>]>].outline>
  - else:
    - determine <[loc].add[<[vec_2]>].to_cuboid[<[loc].add[<[vec]>]>].outline>

#Input 1: LocationTag
#Input 2: Vector offset such as 5,1,2
dcutscene_sphere_proc:
  type: procedure
  debug: false
  definitions: loc|vector
  script:
  - define vector <[vector]||5,5,5>
  - determine <[loc].to_ellipsoid[<[vector]>].shell>

#Input 1: LocationTag
#Input 2: Vector offset such as 2,4,1
#Input 3: Time it takes to interpolate to the next point
#Input 4: Interpolation method linear or smooth
#Input 5: The offset this is useful for a branch type of structure
#Input 6: Another vector this should be used when interpolation is on smooth
#Input 7: Another vector this should be used when interpolation is on smooth
dcutscene_vector_path:
  type: procedure
  debug: false
  definitions: loc|vec|time|interpolation|offset|vec_2|vec_3
  script:
  - define vec <[vec]||null>
  - if <[vec]> == null:
    - determine <[loc]>
  - else:
    - define offset <[offset]||0,0,0>
    - define time <duration[<[time]>].in_ticks>
    - define interpolation <[interpolation]||linear>
    - define loc_1 <[loc]>
    - define loc_2 <[loc].add[<[vec]>]>
    - choose <[interpolation]>:
      - case linear:
        - repeat <[time]>:
          - define time_index <[value]>
          - if <[time_index]> < <[time]>:
            - define time_percent <[time_index].div[<[time]>]>
            - define data <[loc_2].as[location].sub[<[loc_1]>].mul[<[time_percent]>].add[<[loc_1]>]>
            - define points:->:<[data].random_offset[<[offset]>]>
        - determine <[points]||<[loc]>>
      - case smooth:
        - define loc_3 <[loc].add[<[vec_2]||0,0,0>]>
        - define loc_4 <[loc].add[<[vec_3]||<[vec]>>]>
        - repeat <[time]>:
          - define time_index <[value]>
          - if <[time_index]> < <[time]>:
            - define time_percent <[time_index].div[<[time]>]>
            - define data <proc[dcutscene_catmullrom_proc].context[<[loc_3]>|<[loc_1]>|<[loc_2]>|<[loc_4]>|<[time_percent]>]>
            - define points:->:<[data].random_offset[<[offset]>]>
        - determine <[points]||<[loc]>>