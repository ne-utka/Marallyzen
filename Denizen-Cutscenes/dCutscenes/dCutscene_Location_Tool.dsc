#======== Location Tool =========
# A utility tool used for getting exact locations using the ray trace tag or buttons.
# It is a bit messy atm.

# Event Handler for the location tool
dcutscene_location_tool_events:
    type: world
    debug: false
    events:
      on player quits:
      - if <player.has_flag[dcutscene_location_editor]>:
        - define data <player.flag[dcutscene_location_editor]>
        - define root_ent <[data.root_ent]>
        - if <[root_ent].is_spawned>:
          - define root_type <[data.root_type]>
          - choose <[root_type]>:
            - case player_model:
              - if <[root_ent].is_spawned>:
                - run pmodels_remove_model def:<[root_ent]>
            - case model:
              - if <[root_ent].is_spawned>:
                - run dmodels_delete def:<[root_ent]>
        - run dcutscene_location_tool_return_inv
      after player clicks dcutscene_location_tool_item in dcutscene_inventory_location_tool:
      - run dcutscene_location_toolset_inv
      after player clicks dcutscene_location_tool_ray_trace_item in dcutscene_inventory_location_tool:
      - run dcutscene_location_raytrace_inv
      after player right clicks block with:dcutscene_loc_ray_trace:
      - ratelimit <player> 2t
      - if <player.has_flag[cutscene_modify]>:
        - if <player.has_flag[dcutscene_location_editor]>:
          - define data <player.flag[dcutscene_location_editor]>
          - define root_ent <[data.root_ent]>
          - if <[root_ent].is_spawned>:
            - choose <[data.root_type]>:
              - case player_model:
                - run pmodels_end_animation def:<[root_ent]>
              - case model:
                - run dmodels_end_animation def:<[root_ent]>
          - run dcutscene_location_ray_trace_update
      after player right clicks entity with:dcutscene_loc_ray_trace:
      - ratelimit <player> 2t
      - if <player.has_flag[cutscene_modify]>:
        - if <player.has_flag[dcutscene_location_editor]>:
          - define data <player.flag[dcutscene_location_editor]>
          - define root_ent <[data.root_ent]>
          - if <[root_ent].is_spawned>:
            - choose <[data.root_type]>:
              - case player_model:
                - run pmodels_end_animation def:<[root_ent]>
              - case model:
                - run dmodels_end_animation def:<[root_ent]>
          - run dcutscene_location_ray_trace_update
      after player right clicks block with:dcutscene_loc_ray_trace_dist_add:
      - run dcutscene_location_edit_ray_trace_add_dist
      after player right clicks entity with:dcutscene_loc_ray_trace_dist_add:
      - run dcutscene_location_edit_ray_trace_add_dist
      after player right clicks block with:dcutscene_loc_ray_trace_dist_sub:
      - run dcutscene_location_edit_ray_trace_sub_dist
      after player right clicks entity with:dcutscene_loc_ray_trace_dist_sub:
      - run dcutscene_location_edit_ray_trace_sub_dist
      after player right clicks block with:dcutscene_loc_ray_trace_reverse_model:
      - run dcutscene_location_edit_ray_trace_rotate_model
      after player right clicks entity with:dcutscene_loc_ray_trace_reverse_model:
      - run dcutscene_location_edit_ray_trace_rotate_model
      after player right clicks block with:dcutscene_loc_ray_trace_nonsolids:
      - run dcutscene_location_edit_ray_trace_nonsolids
      after player right clicks entity with:dcutscene_loc_ray_trace_nonsolids:
      - run dcutscene_location_edit_ray_trace_nonsolids
      after player right clicks block with:dcutscene_loc_ray_trace_water:
      - run dcutscene_location_edit_ray_trace_water
      after player right clicks entity with:dcutscene_loc_ray_trace_water:
      - run dcutscene_location_edit_ray_trace_water
      on player right clicks block with:dcutscene_loc_forward:
      - if <player.has_flag[cutscene_modify]>:
        - define offset <player.flag[dcutscene_location_editor.button.offset]||null>
        - if <[offset]> != null:
          - define offset_mul <player.flag[dcutscene_location_editor.button.offset_mul]||0>
          - define offset.z <[offset.z].add[1].mul[<[offset_mul]>]>
          - run dcutscene_location_button_change def:<[offset]>
      on player right clicks block with:dcutscene_loc_backward:
      - if <player.has_flag[cutscene_modify]>:
        - define offset <player.flag[dcutscene_location_editor.button.offset]||null>
        - if <[offset]> != null:
          - define offset_mul <player.flag[dcutscene_location_editor.button.offset_mul]||0>
          - define offset.z <[offset.z].sub[1].mul[<[offset_mul]>]>
          - run dcutscene_location_button_change def:<[offset]>
      on player right clicks block with:dcutscene_loc_up:
      - if <player.has_flag[cutscene_modify]>:
        - define offset <player.flag[dcutscene_location_editor.button.offset]||null>
        - if <[offset]> != null:
          - define offset_mul <player.flag[dcutscene_location_editor.button.offset_mul]||0>
          - define offset.y <[offset.y].add[1].mul[<[offset_mul]>]>
          - run dcutscene_location_button_change def:<[offset]>
      on player right clicks block with:dcutscene_loc_down:
      - if <player.has_flag[cutscene_modify]>:
        - define offset <player.flag[dcutscene_location_editor.button.offset]||null>
        - if <[offset]> != null:
          - define offset_mul <player.flag[dcutscene_location_editor.button.offset_mul]||0>
          - define offset.y <[offset.y].sub[1].mul[<[offset_mul]>]>
          - run dcutscene_location_button_change def:<[offset]>
      on player right clicks block with:dcutscene_loc_right:
      - if <player.has_flag[cutscene_modify]>:
        - define offset <player.flag[dcutscene_location_editor.button.offset]||null>
        - if <[offset]> != null:
          - define offset_mul <player.flag[dcutscene_location_editor.button.offset_mul]||0>
          - define offset.x <[offset.x].sub[1].mul[<[offset_mul]>]>
          - run dcutscene_location_button_change def:<[offset]>
      on player right clicks block with:dcutscene_loc_left:
      - if <player.has_flag[cutscene_modify]>:
        - define offset <player.flag[dcutscene_location_editor.button.offset]||null>
        - if <[offset]> != null:
          - define offset_mul <player.flag[dcutscene_location_editor.button.offset_mul]||0>
          - define offset.x <[offset.x].add[1].mul[<[offset_mul]>]>
          - run dcutscene_location_button_change def:<[offset]>
      on player right clicks block with:dcutscene_loc_mul_add:
      - if <player.has_flag[cutscene_modify]>:
        - define data <player.flag[dcutscene_location_editor]||null>
        - if <[data]> != null:
          - define mul_inc <[data.button.offset_mul].add[0.15]>
          - if <[mul_inc]> < 10.0:
            - flag <player> dcutscene_location_editor.button.offset_mul:<[mul_inc]>
            - actionbar "<red><bold>Offset Multiplier + <[mul_inc]>"
          - else:
            - flag <player> dcutscene_location_editor.button.offset_mul:10.0
            - define mul_inc 10.0
            - actionbar "<red><bold>Offset Multiplier Maximum <[mul_inc]>"
      on player right clicks block with:dcutscene_loc_mul_sub:
      - if <player.has_flag[cutscene_modify]>:
        - define data <player.flag[dcutscene_location_editor]||null>
        - if <[data]> != null:
          - define mul_inc <[data.button.offset_mul].sub[0.15]>
          - if <[mul_inc]> >= 0:
            - flag <player> dcutscene_location_editor.button.offset_mul:<[mul_inc]>
            - actionbar "<blue><bold>Offset Multiplier - <[mul_inc]>"
          - else:
            - flag <player> dcutscene_location_editor.button.offset_mul:0.0
            - define mul_inc 0.0
            - actionbar "<blue><bold>Offset Multiplier Minimum <[mul_inc]>"
      on player right clicks block with:dcutscene_loc_use_yaw:
      - if <player.has_flag[cutscene_modify]>:
        - define data <player.flag[dcutscene_location_editor]||null>
        - if <[data]> != null:
          - define use_yaw <[data.button.use_yaw]||false>
          - choose <[use_yaw]>:
            - case true:
              - define use_yaw false
            - case false:
              - define use_yaw true
          - flag <player> dcutscene_location_editor.button.use_yaw:<[use_yaw]>
          - define display "<dark_purple><bold>Use yaw: <gray><[use_yaw]>"
          - inventory adjust d:<player.inventory> slot:<player.held_item_slot> display:<[display]>

# Task for changing the location based on the location button tool
dcutscene_location_button_change:
    type: task
    debug: false
    definitions: offset
    script:
    - define data <player.flag[dcutscene_location_editor]||null>
    - define use_yaw <[data.button.use_yaw]>
    - if <[use_yaw].equals[false]>:
      - define loc <[data.location].with_pitch[0].relative[<[offset.x]>,<[offset.y]>,<[offset.z]>]>
    - else:
      - define loc <[data.location].with_yaw[<player.location.yaw>].with_pitch[0].relative[<[offset.x]>,<[offset.y]>,<[offset.z]>]>
    - define root <[data.root_ent]||null>
    - if <[root]> != null:
      - teleport <[root]> <[loc]>
      - run pmodels_reset_model_position def:<[root]>
    - flag <player> dcutscene_location_editor.location:<[loc]>

# Updates the ray tracer location tool
dcutscene_location_ray_trace_update:
    type: task
    debug: false
    script:
    - define data <player.flag[dcutscene_location_editor]||null>
    - if <[data]> != null:
      - define ray_trace_bool <[data.ray_trace_bool]>
      - choose <[ray_trace_bool]>:
        - case true:
          - define ray_trace_bool false
          - actionbar "<red><bold>Ray Trace Off"
        - case false:
          - define ray_trace_bool true
      - flag <player> dcutscene_location_editor.ray_trace_bool:<[ray_trace_bool]>
      - if <[ray_trace_bool].is_truthy>:
        - actionbar "<gold><bold>Ray Trace On"
        - while <player.flag[dcutscene_location_editor.ray_trace_bool].if_null[false].is_truthy>:
          - run dcutscene_location_edit_ray_trace def:<player.flag[dcutscene_location_editor.ray_trace_range]||5>
          - wait 1t

# Updates the semi path
dcutscene_location_semi_path_interval:
    type: task
    debug: false
    script:
    - define root <player.flag[dcutscene_location_editor.root_ent]||null>
    - define rate <script[dcutscenes_config].data_key[config].get[cutscene_semi_path_update_interval]||0.5s>
    - if <[root]> != null:
      - if <[root].is_spawned>:
        - while <player.has_flag[dcutscene_location_editor]>:
          - define data <player.flag[dcutscene_location_editor]>
          - define root <[data.root_ent]>
          - if <[root].is_spawned>:
            - define root_type <[data.root_type]>
            - define ray <[data.location]>
            - define tick_data <player.flag[dcutscene_save_data.data]||null>
            - if <[tick_data]> == null:
              - stop
            - run dcutscene_semi_path_show def:<[ray]>|<[tick_data.tick]>|<player.flag[dcutscene_tick_modify.tick].if_null[<player.flag[dcutscene_tick_modify]>]>|<[tick_data.uuid]>
          - wait <[rate]>

# Ray traces the location based on the player's cursor
dcutscene_location_edit_ray_trace:
    type: task
    debug: false
    definitions: max_range
    script:
    - define data <player.flag[dcutscene_location_editor]||null>
    - if <[data]> != null:
      - define nonsolid <[data.ray_trace_passable]>
      - define water <[data.ray_trace_water]>
      - define ray_trace <player.eye_location.ray_trace[range=<[max_range]>;default=air;fluids=<[water]>;nonsolids=<[nonsolid]>]||null>
      - if <[ray_trace]> != null:
        - define root <[data.root_ent]||null>
        - define root_type <[data.root_type]>
        - define ploc <player.location>
        - if <[root]> != null:
          #Rotate Model
          - choose <player.flag[dcutscene_location_editor.reverse_model]>:
            - case true:
              - define yaw <[ploc].rotate_yaw[180].yaw>
            - case false:
              - define yaw <[ploc].yaw>
          #Special attribute like model, entity, or player skin
          - define attribute <player.flag[dcutscene_location_editor.attribute]>
          #Check if root model is not spawned and chunk is not loaded
          - if !<[root].is_spawned> || !<[root].location.chunk.is_loaded>:
            - run dcutscene_location_tool_model_spawner def.root_type:<[root_type]> def.attribute:<[attribute]> def.loc:<[ploc]>
          #Fallback to check if distance is greater than view_distance or the world does not equal the player's world
          - else if <[root].location.distance[<[ploc]>].horizontal||0> > <player.world.view_distance.mul[14]> || <[root].location.world> != <[ploc].world>:
            - run dcutscene_location_tool_model_spawner def.root_type:<[root_type]> def.attribute:<[attribute]> def.loc:<[ploc]>
          - define ray_loc <[ray_trace].with_yaw[<[yaw]>]>
          - teleport <[root]> <[ray_loc]>
          - flag <player> dcutscene_location_editor.location:<[ray_loc]>
          - choose <[root_type]>:
            - case player_model:
              - run pmodels_reset_model_position def:<[root]>
            - case model:
              - run dmodels_reset_model_position def:<[root]>
        - else:
          - flag <player> dcutscene_location_editor.location:<[root].location>

# Spawns a model animator for the location tool and updates it
dcutscene_location_tool_model_spawner:
    type: task
    debug: false
    definitions: root_type|attribute|loc
    script:
    - choose <[root_type]>:
      - case player_model:
        - define skin <[attribute]>
        - run pmodels_spawn_model def:<[loc]>|<[skin].parsed>|<player> save:spawned
        - define root <entry[spawned].created_queue.determination.first>
        - flag <player> dcutscene_location_editor.root_ent:<[root]>
      - case model:
        - define model <[attribute]>
        - run dmodels_spawn_model def:<[model]>|<[loc]>|256|<player> save:spawned
        - define root <entry[spawned].created_queue.determination.first>
    - flag <player> dcutscene_location_editor.root_ent:<[root]>

# Increases the distance of the ray trace tool
dcutscene_location_edit_ray_trace_add_dist:
    type: task
    debug: false
    script:
    - if <player.has_flag[cutscene_modify]>:
      - define range <player.flag[dcutscene_location_editor.ray_trace_range]>
      - define config_range <script[dcutscenes_config].data_key[config].get[cutscene_loc_tool_ray_dist]||5>
      - if <[range]> < <[config_range]>:
        - define range <[range].add[0.2]>
        - flag <player> dcutscene_location_editor.ray_trace_range:<[range]>
      - else if <[range]> > <[config_range]>:
        - define range <[config_range]>
        - flag <player> dcutscene_location_editor.ray_trace_range:<[range]>
      - actionbar <red><bold><[range]>
      - run dcutscene_location_edit_ray_trace def:<[range]>

# Decreases the distance of the ray trace tool
dcutscene_location_edit_ray_trace_sub_dist:
    type: task
    debug: false
    script:
    - if <player.has_flag[cutscene_modify]>:
      - define range <player.flag[dcutscene_location_editor.ray_trace_range]>
      - if <[range]> > 0:
        - define range <[range].sub[0.2]>
        - flag <player> dcutscene_location_editor.ray_trace_range:<[range]>
      - else if <[range]> < 0:
        - define range 0.2
        - flag <player> dcutscene_location_editor.ray_trace_range:<[range]>
      - actionbar <blue><bold><[range]>
      - run dcutscene_location_edit_ray_trace def:<[range]>

# Determine if the ray trace tool will ignore passable blocks
dcutscene_location_edit_ray_trace_nonsolids:
    type: task
    debug: false
    script:
    - if <player.has_flag[cutscene_modify]>:
      - define data <player.flag[dcutscene_location_editor]||null>
      - if <[data]> != null:
        - choose <[data.ray_trace_passable]>:
          - case true:
            - define nonsolid false
          - case false:
            - define nonsolid true
        - flag <player> dcutscene_location_editor.ray_trace_passable:<[nonsolid]>
        - inventory set d:<player.inventory> o:dcutscene_loc_ray_trace_nonsolids slot:4

# Determine if the ray trace tool will ignore fluids
dcutscene_location_edit_ray_trace_water:
    type: task
    debug: false
    script:
    - if <player.has_flag[cutscene_modify]>:
      - define data <player.flag[dcutscene_location_editor]||null>
      - if <[data]> != null:
        - choose <[data.ray_trace_water]>:
          - case true:
            - define water false
          - case false:
            - define water true
        - flag <player> dcutscene_location_editor.ray_trace_water:<[water]>
        - inventory set d:<player.inventory> o:dcutscene_loc_ray_trace_water slot:5

# Rotates the model 180 degrees
dcutscene_location_edit_ray_trace_rotate_model:
    type: task
    debug: false
    script:
    - if <player.has_flag[cutscene_modify]>:
      - define data <player.flag[dcutscene_location_editor]||null>
      - if <[data]> != null:
        - define reverse_model <[data.reverse_model]>
        - choose <[reverse_model]>:
          - case true:
            - define reverse_model false
          - case false:
            - define reverse_model true
        - flag <player> dcutscene_location_editor.reverse_model:<[reverse_model]>
        - inventory set d:<player.inventory> o:dcutscene_loc_ray_trace_reverse_model slot:6

# Sets up the player with the data for the location tool
dcutscene_location_tool_give_data:
    type: task
    debug: false
    definitions: loc|root_ent|yaw|type|attribute
    script:
    - define loc <location[<[loc]>]||null>
    - if <[loc]> == null:
      - debug error "Must specify location for location tool"
      - stop
    - define attribute <[attribute]||null>
    - definemap offset x:0.0 y:0.0 z:0.0
    - if <[yaw].exists>:
      - define yaw <[yaw]>
    - else:
      - define yaw 0
    - definemap button_data offset:<[offset]> offset_mul:1 yaw:<[yaw]> location:<[loc]> use_yaw:true
    - definemap editor_data button:<[button_data]> ray_trace_bool:false ray_trace_solids:false ray_trace_passable:false ray_trace_water:true reverse_model:false
    - define editor_data.ray_trace_range <script[dcutscenes_config].data_key[config].get[cutscene_loc_tool_ray_dist]||5>
    - flag <player> dcutscene_location_editor:<[editor_data]>
    - if <entity[<[root_ent]>]||null> != null:
      - flag <player> dcutscene_location_editor.root_ent:<[root_ent]>
      - flag <player> dcutscene_location_editor.location:<[root_ent].location>
    - if <[type]||null> != null:
      - flag <player> dcutscene_location_editor.root_type:<[type]>
    - if <[attribute]||null> != null:
      - flag <player> dcutscene_location_editor.attribute:<[attribute]>
    - flag <player> dcutscene_location_editor.inv:<player.inventory.map_slots>
    - run dcutscene_location_semi_path_interval

# Location Button Tool Inventory
dcutscene_location_toolset_inv:
    type: task
    debug: false
    script:
    - define inv <player.inventory>
    - inventory set d:<[inv]> o:dcutscene_loc_forward slot:1
    - inventory set d:<[inv]> o:dcutscene_loc_backward slot:2
    - inventory set d:<[inv]> o:dcutscene_loc_up slot:3
    - inventory set d:<[inv]> o:dcutscene_loc_down slot:4
    - inventory set d:<[inv]> o:dcutscene_loc_left slot:5
    - inventory set d:<[inv]> o:dcutscene_loc_right slot:6
    - inventory set d:<[inv]> o:dcutscene_loc_mul_sub slot:7
    - inventory set d:<[inv]> o:dcutscene_loc_mul_add slot:8
    - inventory set d:<[inv]> o:dcutscene_loc_use_yaw slot:9
    - inventory close

# Returns the previous inventory
dcutscene_location_tool_return_inv:
    type: task
    debug: false
    script:
    - inventory swap d:<player.inventory> o:<player.flag[dcutscene_location_editor.inv]>

# Ray Trace Tool Inventory
dcutscene_location_raytrace_inv:
    type: task
    debug: false
    script:
    - define inv <player.inventory>
    - inventory set d:<[inv]> o:dcutscene_loc_ray_trace slot:1
    - inventory set d:<[inv]> o:dcutscene_loc_ray_trace_dist_add slot:2
    - inventory set d:<[inv]> o:dcutscene_loc_ray_trace_dist_sub slot:3
    - inventory set d:<[inv]> o:dcutscene_loc_ray_trace_nonsolids slot:4
    - inventory set d:<[inv]> o:dcutscene_loc_ray_trace_water slot:5
    - inventory set d:<[inv]> o:dcutscene_loc_ray_trace_reverse_model slot:6
    - inventory close
    - adjust <player> item_slot:1