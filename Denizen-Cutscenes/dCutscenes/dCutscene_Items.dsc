#(Do not change the item script names please you are free to change anything else)

#The visible camera when editing cutscenes
dcutscene_camera_item:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10010

dcutscene_open_gui_item:
    type: item
    material: scute
    display name: <blue><bold>Open Cutscene Editor
    mechanisms:
      custom_model_data: 10015

dcutscene_play_cutscene_item:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10008
    display name: <blue><bold>Play Cutscene
    lore:
    - <empty>
    - <gray><italic>Click to play this cutscene

dcutscene_save_file_item:
    type: item
    material: enchanted_book
    display name: <blue><bold>Save Cutscene
    lore:
    - <empty>
    - <gray>Saves the cutscene to a directory
    - <gray>for use in other servers or data backup.
    - <green>Denizen/data/dcutscenes/scenes

dcutscene_add_new_model:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10009
    display name: <green><bold>Add New Model +
    lore:
    - <empty>
    - <gray><italic>Click to add new model

#The exit button for the cutscene gui
dcutscene_exit:
    type: item
    material: scute
    display name: <red><bold>Exit
    mechanisms:
      custom_model_data: 10007

#Next page button
dcutscene_next:
    type: item
    material: scute
    display name: <blue><bold>Next Page
    mechanisms:
        custom_model_data: 10003

#Previous page button
dcutscene_previous:
    type: item
    material: scute
    display name: <blue><bold>Previous Page
    mechanisms:
        custom_model_data: 10002

#Scroll down the page
dcutscene_scroll_down:
    type: item
    material: scute
    display name: <blue><bold>Scroll Down
    mechanisms:
        custom_model_data: 10001

#Scroll up the page
dcutscene_scroll_up:
    type: item
    material: scute
    display name: <blue><bold>Scroll Up
    mechanisms:
        custom_model_data: 10004

#Keyframe Item
dcutscene_keyframe:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10012

#Keyframe that contains elements
dcutscene_keyframe_contains:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10013

#Keyframe that contains the scene stop point
dcutscene_keyframe_stop_point:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10014

#Sub keyframe
dcutscene_sub_keyframe:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10011

#Create new cutscene item
dcutscene_new_scene_item:
    type: item
    material: scute
    display name: <green><bold>New Scene +
    lore:
    - <gray>Create a new cutscene
    mechanisms:
      custom_model_data: 10009

#back page button
dcutscene_back_page:
    type: item
    material: scute
    display name: <blue><bold>Back
    mechanisms:
        custom_model_data: 10000

###### Settings #########
#Settings button
dcutscene_settings:
    type: item
    material: smithing_table
    display name: <dark_gray><bold>Settings
    lore:
    - <empty>
    - <gray>Change the settings of this cutscene.

dcutscene_change_scene_name:
    type: item
    material: name_tag
    display name: <green><bold>Change Scene Name
    lore:
    - <empty>
    - <gray><italic>Click to change scene name

dcutscene_change_description_item:
    type: item
    material: book
    display name: <aqua><bold>Change Description
    lore:
    - <empty>
    - <gray><italic>Click to change description

dcutscene_change_show_bars:
    type: item
    material: black_wool
    display name: <blue><bold>Show Cutscene Bars
    lore:
    - <empty>
    - <gray>Determine whether the cutscene bars
    - <gray>should be shown or not.
    - <empty>
    - <gray><italic>Click to change cutscene bars

dcutscene_duplicate_scene:
    type: item
    material: shulker_shell
    display name: <dark_blue><bold>Duplicate Cutscene
    lore:
    - <empty>
    - <gray><italic>Click to duplicate this cutscene

dcutscene_change_item:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10015
    display name: <green><bold>Change GUI Item
    lore:
    - <empty>
    - <gray><italic>Click to change GUI item

dcutscene_hide_players:
    type: item
    material: daylight_detector
    display name: <gold><bold>Hide Players
    lore:
    - <empty>
    - <gray>Determine if players will
    - <gray>be hidden during the cutscene.
    - <empty>
    - <gray><italic>Click to change hide players

dcutscene_bound_to_camera:
    type: item
    material: armor_stand
    display name: <dark_gray><bold>Bound To Camera
    lore:
    - <empty>
    - <gray>If true the player will stay
    - <gray>near the camera and cannot do normal
    - <gray>activity.
    - <empty>
    - <gray>Note: If false and the player is too
    - <gray>far from the camera this can cause
    - <gray>issues this was created so an external
    - <gray>source can modify the player without being
    - <gray>bound to the camera.
    - <empty>
    - <gray><italic>Click to change bound to camera

dcutscene_delete_cutscene:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10005
    display name: <red><bold>Delete Cutscene
    lore:
    - <empty>
    - <gray>This will delete the cutscene entirely
    - <gray>from the server. It is recommended you make
    - <gray>a backup using the save file button.
    - <empty>
    - <gray><italic>Click to delete this cutscene

dcutscene_set_origin_point:
    type: item
    material: recovery_compass
    display name: <green><bold>Set Origin Point
    lore:
    - <empty>
    - <gray>Set the origin point of the cutscene.
    - <gray>All animator locations will be
    - <gray>calculated relative to this point.
    - <gray>You may use a tag for the input note that
    - <gray>if the location does not exist it will return to
    - <gray>the cutscene's default location.
    - <empty>
    - <gray><italic>Click to set origin point

dcutscene_origin_point_refresh:
    type: item
    material: fishing_rod
    display name: <green><bold>Refresh Offsets
    lore:
    - <empty>
    - <gray>Refreshes the location offsets of each
    - <gray>animator that contains locations.
    - <empty>
    - <gray><italic>Click to refresh offsets

###### Cutscene GUI Items ########
#Modify keyframes button
dcutscene_keyframes_list:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10012
    display name: <blue><bold>Modify Keyframes

#Add animator button
dcutscene_keyframe_tick_add:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10009
    display name: <green><bold>Add Animator +

#Default item for cutscenes
dcutscene_scene_item_default:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10015

##Location Tool Items ##########
dcutscene_location_tool_item:
    type: item
    material: ender_chest
    display name: <green><bold>Use Location Tool
    lore:
    - <empty>
    - <gray>A utility tool that assists in modifying locations.
    - <empty>
    - <gray><italic>Click to use location tool

dcutscene_location_tool_confirm_location:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10017
    display name: <green><bold>Confirm Location

dcutscene_location_tool_ray_trace_item:
    type: item
    material: spectral_arrow
    display name: <gold><bold>Ray Trace Location
    lore:
    - <empty>
    - <gray>Adjusts the location based on your cursor
    - <gray>generally you should use this.
    - <empty>
    - <gray><italic>Click to use ray trace location tool

#Up Button
dcutscene_loc_up:
    type: item
    material: purple_stained_glass_pane
    display name: <blue><bold>UP

dcutscene_loc_down:
    type: item
    material: orange_stained_glass_pane
    display name: <gold><bold>DOWN

dcutscene_loc_right:
    type: item
    material: green_stained_glass_pane
    display name: <green><bold>Right

dcutscene_loc_left:
    type: item
    material: red_stained_glass_pane
    display name: <red><bold>Left

dcutscene_loc_forward:
    type: item
    material: blue_stained_glass_pane
    display name: <blue><bold>Forward

dcutscene_loc_backward:
    type: item
    material: pink_stained_glass_pane
    display name: <light_purple><bold>Backward

dcutscene_loc_mul_add:
    type: item
    material: redstone_torch
    display name: <red><bold>Increase Multiplier +

dcutscene_loc_mul_sub:
    type: item
    material: warped_pressure_plate
    display name: <blue><bold>Decrease Multiplier -

dcutscene_loc_use_yaw:
    type: item
    material: comparator
    display name: <dark_purple><bold>Use Yaw <gray><player.flag[dcutscene_location_editor.button.use_yaw]>

dcutscene_loc_ray_trace:
    type: item
    material: spectral_arrow
    display name: <gold><bold>Ray Trace

dcutscene_loc_ray_trace_dist_add:
    type: item
    material: red_wool
    display name: <red><bold>Increase Ray Distance + <player.flag[dcutscene_location_editor.ray_trace_range]>

dcutscene_loc_ray_trace_dist_sub:
    type: item
    material: blue_wool
    display name: <blue><bold>Decrease Ray Distance - <player.flag[dcutscene_location_editor.ray_trace_range]>

dcutscene_loc_ray_trace_solid:
    type: item
    material: stone
    display name: <dark_purple><bold>Ignore Solids <gray><player.flag[dcutscene_location_editor.ray_trace_solids]>

dcutscene_loc_ray_trace_nonsolids:
    type: item
    material: grass
    display name: <green><bold>Track Passable Blocks <gray><player.flag[dcutscene_location_editor.ray_trace_passable]>

dcutscene_loc_ray_trace_water:
    type: item
    material: water_bucket
    display name: <dark_blue><bold>Track Fluids <gray><player.flag[dcutscene_location_editor.ray_trace_water]>

dcutscene_loc_ray_trace_reverse_model:
    type: item
    material: recovery_compass
    display name: <aqua><bold>Reverse Model <gray><player.flag[dcutscene_location_editor.reverse_model]>

#======== Option Items ===========
##Camera
dcutscene_add_cam:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10010
    display name: <dark_gray><bold>Modify Camera
    lore:
    - <empty>
    - <gray>Change where the camera moves or looks

dcutscene_camera_path_show:
    type: item
    material: <script[dcutscenes_config].data_key[config].get[cutscene_path_material]||barrier>
    display name: <dark_aqua><bold>Camera Path
    lore:
    - <gray>Shows the camera's path in the cutscene

dcutscene_camera_keyframe:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10010
    display name: <dark_gray><bold>Camera

dcutscene_camera_loc_modify:
    type: item
    material: book
    display name: <blue><bold>Change Camera Location
    lore:
    - <empty>
    - <gray><italic>Click to change location of camera in keyframe

dcutscene_camera_teleport:
    type: item
    material: nether_star
    display name: <aqua><bold>Teleport to location

dcutscene_camera_interp_modify:
    type: item
    material: lead
    display name: <green><bold>Path Interpolation Method
    lore:
    - <empty>
    - <gray>The interpolation method for the movement
    - <gray>path of the camera
    - <gray>Linear: A straight line (Linear Interpolation)
    - <gray>Smooth: A spline curve (Centripetal Catmullrom Interpolation)
    - <empty>
    - <gray><italic>Click to modify interpolation method

dcutscene_camera_move_modify:
    type: item
    material: feather
    display name: <green><bold>Move Camera
    lore:
    - <empty>
    - <gray>Determine if the camera will move to the next keyframe point
    - <empty>
    - <gray><italic>Click to modify movement for camera

dcutscene_camera_upside_down:
    type: item
    material: magenta_glazed_terracotta
    display name: <dark_purple><bold>Invert Look
    lore:
    - <empty>
    - <gray>Determine if the camera is upside down or not.
    - <empty>
    - <red>EXPIREMENTAL
    - <empty>
    - <gray><italic>Click to change invert look

dcutscene_camera_look_modify:
    type: item
    material: spyglass
    display name: <dark_green><bold>Set look location
    lore:
    - <empty>
    - <gray>Set the look location for the
    - <gray>camera in this keyframe.
    - <gray>Input <red>false <gray>to disable.
    - <empty>
    - <gray><italic>Click to set the look location

dcutscene_camera_rotate_modify:
    type: item
    material: redstone_torch
    display name: <dark_aqua><bold>Rotate Multiplier
    lore:
    - <empty>
    - <gray>Determine how fast the camera
    - <gray>will rotate and look at the next
    - <gray>keyframe point.
    - <empty>
    - <gray><italic>Click to change rotate view

dcutscene_camera_interpolate_look:
    type: item
    material: compass
    display name: <dark_green><bold>Interpolate Look
    lore:
    - <empty>
    - <gray>If true the camera will smoothly
    - <gray>transition to the next look point.
    - <gray>If false the camera will stay at
    - <gray>the previous keyframe point yaw and pitch.
    - <empty>
    - <gray><italic>Click to modify look interpolation for camera

dcutscene_camera_record_player:
    type: item
    material: redstone_block
    display name: <red><bold>Record Movement
    lore:
    - <empty>
    - <gray>Records your movement for
    - <gray>the duration specified.
    - <gray>This should generally be used
    - <gray>for close up precise shots.
    - <empty>
    - <gray><italic>Click to record movement

dcutscene_camera_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the camera animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move camera animator

dcutscene_camera_duplicate_to_keyframe:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the camera animator and
    - <gray>move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate camera animator

dcutscene_camera_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_camera_remove_modify:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10006
    display name: <red><bold>Remove camera from keyframe
    lore:
    - <empty>
    - <gray><italic>Click to remove camera from keyframe
########

## Regular Entity ###
dcutscene_add_entity:
    type: item
    material: zombie_head
    display name: <green><bold>Show entity
    lore:
    - <empty>
    - <blue><italic><underline>Planned

## Denizen Models ###
dcutscene_add_model:
    type: item
    material: dragon_head
    display name: <red><bold>Show Model
    lore:
    - <empty>
    - <gray>Show a model to the player and play animations with it.
    - <blue>Requires DModels

dcutscene_model_keyframe_item:
    type: item
    display name: <red><bold>Model
    material: dragon_head

dcutscene_model_change_id:
    type: item
    material: name_tag
    display name: <green><bold>Change ID
    lore:
    - <empty>
    - <gray><italic>Click to change model ID

dcutscene_model_change_item:
    type: item
    material: dragon_head
    display name: <blue><bold>Change Model Item
    lore:
    - <empty>
    - <gray>Change the item displayed for
    - <gray>this model in the cutscene GUI.
    - <empty>
    - <gray>Click to change displayed item

dcutscene_model_change_model:
    type: item
    material: sculk_shrieker
    display name: <dark_blue><bold>Change Model
    lore:
    - <empty>
    - <gray>Change the model spawned.
    - <gray>Note: Must be a starting point
    - <gray>model (root-model).
    - <empty>
    - <gray><italic>Click to change model

dcutscene_model_change_location:
    type: item
    material: recovery_compass
    display name: <dark_green><bold>Change location
    lore:
    - <empty>
    - <gray><italic>Click to change model location

dcutscene_model_ray_trace_change:
    type: item
    material: spectral_arrow
    display name: <red><bold>Ray Trace
    lore:
    - <empty>
    - <gray>Determine if the model will be on
    - <gray>the floor or ceiling during movement
    - <gray>generally this should be kept
    - <gray>true unless not needed such as
    - <gray>a flying model.
    - <empty>
    - <gray><italic>Click to change ray trace

dcutscene_model_ray_trace_determine:
    type: item
    material: spectral_arrow
    display name: <red><bold>Ray Trace Direction
    lore:
    - <empty>
    - <gray>Specify whether the path will
    - <gray>ray trace the floor, ceiling,
    - <gray>or neither.
    - <empty>
    - <gray><italic>Click to modify model ray trace

dcutscene_model_ray_trace_liquid:
    type: item
    material: water_bucket
    display name: <blue><bold>Ray Trace Liquid
    lore:
    - <empty>
    - <gray>If true the model will move
    - <gray>on the surface of liquids.
    - <gray>If false the ray will
    - <gray>ignore liquids and go through.
    - <empty>
    - <gray><italic>Click to modify ray trace liquid

dcutscene_model_ray_trace_passable:
    type: item
    material: grass
    display name: <green><bold>Ray Trace Passable
    lore:
    - <empty>
    - <gray>If true the model will move
    - <gray>on passable blocks such as grass.
    - <gray>If false the ray will ignore
    - <gray>passable blocks.
    - <empty>
    - <gray><italic>Click to modify ray trace passable

dcutscene_model_change_move:
    type: item
    material: saddle
    display name: <blue><bold>Move Model
    lore:
    - <empty>
    - <gray>Determine if the model will move to next keyframe point.
    - <empty>
    - <gray><italic>Click to change model move

dcutscene_model_change_animation:
    type: item
    material: enchanting_table
    display name: <dark_blue><bold>Change Animation
    lore:
    - <empty>
    - <gray>Play an animation for the
    - <gray>model or stop one from playing.
    - <empty>
    - <gray><italic>Click to change animation

dcutscene_model_interp_method:
    type: item
    material: lead
    display name: <yellow><bold>Interpolation Method
    lore:
    - <empty>
    - <gray>The interpolation method for the movement
    - <gray>path of the player model
    - <gray>Linear: A straight line (Linear Interpolation)
    - <gray>Smooth: A spline curve (Centripetal Catmullrom Interpolation)
    - <empty>
    - <gray><italic>Click to change path interpolation method

dcutscene_model_show_path:
    type: item
    material: <script[dcutscenes_config].data_key[config].get[cutscene_path_material]||barrier>
    display name: <blue><bold>Show Model Path
    lore:
    - <empty>
    - <gray>Shows the model's movement path in the cutscene.
    - <empty>
    - <gray><italic>Click to show model path

dcutscene_model_interp_rotate_change:
    type: item
    material: compass
    display name: <dark_purple><bold>Rotate Interpolation
    lore:
    - <empty>
    - <gray>Determine if the model will rotate
    - <gray>smoothly to the next keyframe point.
    - <empty>
    - <gray><italic>Click to change path rotation interpolation

dcutscene_model_interp_rotate_mul:
    type: item
    material: redstone_torch
    display name: <light_purple><bold>Rotate Multiplier
    lore:
    - <empty>
    - <gray>Determine how fast the model
    - <gray>will rotate to the next keyframe point
    - <gray>if rotate interpolation is true.
    - <empty>
    - <gray><italic>Click to change rotate multiplier

dcutscene_model_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the model animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move model animator

dcutscene_model_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the model
    - <gray>animator and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate model animator

dcutscene_model_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_model_teleport_loc:
    type: item
    material: nether_star
    display name: <aqua><bold>Teleport to location
    lore:
    - <empty>
    - <gray><italic>Click to teleport to model location

dcutscene_remove_model_tick:
    type: item
    material: scute
    display name: <red><bold>Remove from tick
    mechanisms:
        custom_model_data: 10006
    lore:
    - <empty>
    - <gray><italic>Click to remove model from this tick

dcutscene_remove_model:
    type: item
    material: scute
    display name: <dark_red><bold>Remove Model
    mechanisms:
        custom_model_data: 10005
    lore:
    - <empty>
    - <gray><italic>Click to remove model from cutscene

## Player Model ###
dcutscene_add_player_model:
    type: item
    material: player_head
    display name: <blue><bold>Show Player Model
    mechanisms:
      skull_skin: <player.skull_skin>
    lore:
    - <empty>
    - <gray>Show a player model of the player and play animations with it.
    - <blue>Requires Denizen Player Models

dcutscene_keyframe_player_model:
    type: item
    material: player_head
    display name: <blue><bold>Player Model

dcutscene_player_model_change_id:
    type: item
    material: name_tag
    display name: <blue><bold>Change ID
    lore:
    - <empty>
    - <gray><italic>Click to change player model ID

dcutscene_player_model_change_animation:
    type: item
    material: enchanting_table
    display name: <dark_blue><bold>Animation
    lore:
    - <empty>
    - <gray>Play an animation for the player
    - <gray>model or stop one from playing.
    - <empty>
    - <gray><italic>Click to change animation

dcutscene_player_model_change_skin:
    type: item
    material: player_head
    display name: <red><bold>Change Player Skin
    mechanisms:
      skull_skin: <player.skull_skin>
    lore:
    - <empty>
    - <gray>Change the skin of the player
    - <gray>model you may use an npc or any
    - <gray>valid tag that contains a player.
    - <empty>
    - <gray><italic>Click to change player model skin

dcutscene_player_model_change_move:
    type: item
    material: saddle
    display name: <gold><bold>Move Player Model
    lore:
    - <empty>
    - <gray>Determine if the player model will move to next keyframe point.
    - <empty>
    - <gray><italic>Click to change player model move

dcutscene_player_model_change_location:
    type: item
    material: recovery_compass
    display name: <green><bold>Change Location
    lore:
    - <empty>
    - <gray><italic>Click to change player model location

dcutscene_player_model_ray_trace_change:
    type: item
    material: spectral_arrow
    display name: <dark_aqua><bold>Ray Trace
    lore:
    - <empty>
    - <gray>Determine if the model will be on
    - <gray>the floor or ceiling during movement
    - <gray>generally this should be kept
    - <gray>true unless not needed such as
    - <gray>a flying model.
    - <empty>
    - <gray><italic>Click to change ray trace

dcutscene_player_model_move:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move Model
    lore:
    - <empty>
    - <gray>Determine if the player model will move to the next keyframe point
    - <empty>
    - <gray><italic>Click to change player model move

dcutscene_player_model_teleport_loc:
    type: item
    material: nether_star
    display name: <aqua><bold>Teleport to location
    lore:
    - <empty>
    - <gray><italic>Click to teleport to player model location

dcutscene_player_model_interp_method:
    type: item
    material: lead
    display name: <blue><bold>Path Interpolation Method
    lore:
    - <empty>
    - <gray>The interpolation method for the movement
    - <gray>path of the player model
    - <gray>Linear: A straight line (Linear Interpolation)
    - <gray>Smooth: A spline curve (Centripetal Catmullrom Interpolation)
    - <empty>
    - <gray><italic>Click to change path interpolation method

dcutscene_player_model_interp_rotate_change:
    type: item
    material: compass
    display name: <dark_purple><bold>Rotate Interpolation
    lore:
    - <empty>
    - <gray>Determine if the player model will
    - <gray>rotate smoothly to the next keyframe point.
    - <empty>
    - <gray><italic>Click to change path rotate interpolation

dcutscene_player_model_interp_rotate_mul:
    type: item
    material: redstone_torch
    display name: <light_purple><bold>Rotate Multiplier
    lore:
    - <empty>
    - <gray>Determine how fast the player
    - <gray>will rotate to the next keyframe point
    - <gray>if rotate interpolation is true.
    - <empty>
    - <gray><italic>Click to change rotate multiplier

dcutscene_player_model_show_path:
    type: item
    material: <script[dcutscenes_config].data_key[config].get[cutscene_path_material]||barrier>
    display name: <blue><bold>Show Player Model Path
    lore:
    - <empty>
    - <gray>Shows the player model's movement path in the cutscene.
    - <empty>
    - <gray><italic>Click to show player model path

dcutscene_player_model_ray_trace_determine:
    type: item
    material: spectral_arrow
    display name: <red><bold>Ray Trace Direction
    lore:
    - <empty>
    - <gray>Specify whether the path will
    - <gray>ray trace the floor, ceiling,
    - <gray>or neither.
    - <empty>
    - <gray><italic>Click to modify player model ray trace

dcutscene_player_model_ray_trace_liquid:
    type: item
    material: water_bucket
    display name: <blue><bold>Ray Trace Liquid
    lore:
    - <empty>
    - <gray>If true the player model will move
    - <gray>on the surface of liquids.
    - <gray>If false the ray will
    - <gray>ignore liquids and go through.
    - <empty>
    - <gray><italic>Click to modify ray trace liquid

dcutscene_player_model_ray_trace_passable:
    type: item
    material: grass
    display name: <green><bold>Ray Trace Passable
    lore:
    - <empty>
    - <gray>If true the player model will move
    - <gray>on passable blocks such as grass.
    - <gray>If false the ray will ignore
    - <gray>passable blocks.
    - <empty>
    - <gray><italic>Click to modify ray trace passable

dcutscene_player_model_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the player model animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move player model animator

dcutscene_player_model_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the player model
    - <gray>animator and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate player model animator

dcutscene_player_model_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_remove_player_model:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10005
    display name: <dark_red><bold>Remove Player Model
    lore:
    - <empty>
    - <gray><italic>Click to remove player model from cutscene

dcutscene_remove_player_model_tick:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove from tick
    lore:
    - <empty>
    - <gray><italic>Click to remove player model from this tick

##Run Task ###
dcutscene_add_run_task:
    type: item
    material: enchanted_book
    display name: <dark_purple><bold>Run a task
    lore:
    - <empty>
    - <gray>Run a task script.
    - <blue>Example: run give_rewards

dcutscene_run_task_keyframe:
    type: item
    material: enchanted_book
    display name: <dark_purple><bold>Run Task

dcutscene_run_task_change_modify:
    type: item
    material: enchanted_book
    display name: <dark_purple><bold>Change Run Task Script
    lore:
    - <empty>
    - <gray><italic>Click to change run task script

dcutscene_run_task_def_modify:
    type: item
    material: writable_book
    display name: <dark_blue><bold>Set Definitions
    lore:
    - <empty>
    - <gray>Pass definitions through the
    - <gray>run task.
    - <empty>
    - <gray><italic>Click to set definitions

dcutscene_run_task_wait_modify:
    type: item
    material: clock
    display name: <gold><bold>Waitable Task
    lore:
    - <empty>
    - <gray>Determine if the run task will
    - <gray>will be performed in a slowed
    - <gray>execution. (Useful for resource
    - <gray>intensive tasks ensuring the server
    - <gray>does not freeze.)
    - <empty>
    - <gray><italic>Click to change waitable

dcutscene_run_task_delay_modify:
    type: item
    material: repeater
    display name: <aqua><bold>Task Delay
    lore:
    - <empty>
    - <gray>Delays the script before
    - <gray>it runs.
    - <empty>
    - <gray><italic>Click to change run task delay

dcutscene_run_task_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the run task animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move run task animator

dcutscene_run_task_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the run task animator and
    - <gray>move it to a new tick.

dcutscene_run_task_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_run_task_remove_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Run Task
    lore:
    - <empty>
    - <gray><italic>Click to remove run task

##############

##Fake Block
dcutscene_add_fake_block:
    type: item
    material: stone
    display name: <aqua><bold>Fake Block
    lore:
    - <empty>
    - <gray>Display a fake block
    - <gray>that is only visible
    - <gray>to the player.
    - <empty>
    - <gray><italic>Click to add fake block

dcutscene_fake_block_keyframe:
    type: item
    material: stone
    display name: <aqua><bold>Fake Block

dcutscene_fake_object_block_select:
    type: item
    material: stone
    display name: <blue><bold>Show a fake block
    lore:
    - <empty>
    - <gray>Show a fake block at a location
    - <gray>that only appears for the player
    - <gray>in the cutscene.
    - <empty>
    - <gray><italic>Click to add new fake block

dcutscene_fake_object_block_modify:
    type: item
    material: stone
    display name: <blue><bold>Fake Block
    lore:
    - <empty>
    - <gray><italic>Click to modify fake block

dcutscene_fake_object_block_material_change:
    type: item
    material: stone
    display name: <green><bold>Change Material
    lore:
    - <empty>
    - <gray><italic>Click to change the fake block material

dcutscene_fake_object_block_loc_change:
    type: item
    display name: <dark_purple><bold>Change Location
    material: compass
    lore:
    - <empty>
    - <gray><italic>Click to change the fake block location

dcutscene_fake_object_block_proc_change:
    type: item
    display name: <dark_blue><bold>Set Procedure Script
    material: comparator
    lore:
    - <empty>
    - <gray>Set the procedure script
    - <gray>for the block location.
    - <blue>Definition: <aqua>loc
    - <gray>See the wiki for example usages.
    - <empty>
    - <gray><italic>Click to set procedure

dcutscene_fake_object_block_proc_def_change:
    type: item
    display name: <dark_aqua><bold>Set Procedure Definitions
    material: writable_book
    lore:
    - <empty>
    - <gray>Set definitions to parse for
    - <gray>the procedure script.
    - <gray>See the wiki for example usages.
    - <empty>
    - <gray><italic>Click to set procedure definitions

dcutscene_fake_object_block_duration_change:
    type: item
    material: clock
    display name: <dark_blue><bold>Set Duration
    lore:
    - <empty>
    - <gray>This is how long the fake block
    - <gray>will appear for the player.
    - <empty>
    - <gray><italic>Click to set fake block duration

dcutscene_fake_object_block_teleport:
    type: item
    material: nether_star
    display name: <blue><bold>Teleport to location
    lore:
    - <empty>
    - <gray><italic>Click to teleport to location

dcutscene_fake_block_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the fake block animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move fake block animator

dcutscene_fake_block_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the fake block animator
    - <gray>and move it to a tick.
    - <empty>
    - <gray><italic>Click to duplicate fake block animator

dcutscene_fake_block_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_fake_object_block_remove:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Fake Block
    lore:
    - <empty>
    - <gray><italic>Click to remove fake block

##Fake Schem
dcutscene_add_fake_schem:
    type: item
    material: scaffolding
    display name: <dark_aqua><bold>Fake Schematic
    lore:
    - <empty>
    - <gray>Display a pasted schematic that only
    - <gray>the player can see for a specified
    - <gray>duration.
    - <empty>
    - <gray><italic>Click to add fake schematic

dcutscene_fake_schem_keyframe:
    type: item
    material: scaffolding
    display name: <dark_aqua><bold>Fake Schematic

dcutscene_fake_object_schem_select:
    type: item
    material: hopper
    display name: <red><bold>Show a fake schematic
    lore:
    - <empty>
    - <gray>Show a fake pasted schematic at a location
    - <gray>that only appears for the player
    - <gray>in the cutscene.
    - <empty>
    - <gray><italic>Click to add new fake schematic

dcutscene_fake_object_schem_modify:
    type: item
    material: hopper
    display name: <red><bold>Fake Schematic
    lore:
    - <empty>
    - <gray><italic>Click to modify fake schematic

dcutscene_fake_object_schem_name_change:
    type: item
    material: name_tag
    display name: <blue><bold>Change Schematic Name
    lore:
    - <empty>
    - <gray>This will change the schematic.
    - <empty>
    - <gray><italic>Click to change schematic name

dcutscene_fake_object_schem_loc_change:
    type: item
    material: compass
    display name: <red><bold>Change Location
    lore:
    - <empty>
    - <gray><italic>Click to change schematic paste location

dcutscene_fake_object_schem_duration_change:
    type: item
    material: clock
    display name: <green><bold>Schematic Duration
    lore:
    - <empty>
    - <gray>How long the schematic will
    - <gray>appear for the player.
    - <empty>
    - <gray><italic>Click to change schematic duration

dcutscene_fake_object_schem_noair_change:
    type: item
    material: barrier
    display name: <aqua><bold>No Air
    lore:
    - <empty>
    - <gray>Determine whether the
    - <gray>schematic will be pasted with no air.
    - <empty>
    - <gray><italic>Click to change schematic noair

dcutscene_fake_object_schem_waitable_change:
    type: item
    material: repeater
    display name: <gold><bold>Waitable
    lore:
    - <empty>
    - <gray>Determine if the schematic
    - <gray>will be pasted with a delay.
    - <gray>This should be used for large
    - <gray>schematics.
    - <empty>
    - <gray><italic>Click to change schematic waitable

dcutscene_fake_object_schem_angle_change:
    type: item
    material: piston
    display name: <dark_blue><bold>Schematic Direction
    lore:
    - <empty>
    - <gray>The direction the schematic
    - <gray>will be pasted.
    - <empty>
    - <gray><italic>Click to change schematic paste direction

dcutscene_fake_object_schem_mask_change:
    type: item
    material: tinted_glass
    display name: <gray><bold>Mask Material
    lore:
    - <empty>
    - <gray>Specify what blocks will be
    - <gray>limited when pasting the schematic.
    - <empty>
    - <gray><italic>Click to change schematic mask

dcutscene_fake_object_schem_teleport:
    type: item
    material: nether_star
    display name: <dark_aqua><bold>Teleport to location
    lore:
    - <empty>
    - <gray><italic>Click to teleport to schematic location

dcutscene_fake_schem_move_to:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the fake schematic animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move fake schematic animator

dcutscene_fake_schem_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the fake schematic animator
    - <gray>and move it to a new tick.
    - <empty>
    - <gray><italic>Click to move fake schematic animator

dcutscene_fake_schem_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_fake_object_schem_remove:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Fake Schematic
    lore:
    - <empty>
    - <gray><italic>Click to remove fake schematic

##Screeneffect ###
dcutscene_add_screeneffect:
    type: item
    material: lectern
    display name: <light_purple><bold>Cinematic Screeneffect
    lore:
    - <empty>
    - <gray>Useful for scene transitions.
    - <blue>Requires screeneffect unicode in resource pack

dcutscene_screeneffect_keyframe:
    type: item
    material: lectern
    display name: <light_purple><bold>Cinematic Screeneffect
    lore:
    - <empty>
    - <gray><italic>Click to modify screeneffect

dcutscene_screeneffect_time_modify:
    type: item
    material: note_block
    display name: <blue><bold>Change time
    lore:
    - <empty>
    - <gray><italic>Click to change screeneffect time

dcutscene_screeneffect_color_modify:
    type: item
    material: painting
    display name: <red><bold>Change Color
    lore:
    - <empty>
    - <gray><italic>Click to change screeneffect color

dcutscene_screeneffect_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the screeneffect animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move screeneffect animator

dcutscene_screeneffect_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the screeneffect animator
    - <gray>and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate screeneffect animator

dcutscene_screeneffect_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_screeneffect_remove_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Screeneffect
    lore:
    - <empty>
    - <gray><italic>Click to remove screeneffect
#################

##Particle ########
dcutscene_add_particle:
    type: item
    material: heart_of_the_sea
    display name: <green><bold>Show a particle
    lore:
    - <empty>
    - <gray>Show a particle to the player at a location.

dcutscene_particle_keyframe:
    type: item
    material: heart_of_the_sea
    display name: <green><bold>Particle

dcutscene_particle_modify:
    type: item
    material: heart_of_the_sea
    display name: <green><bold>Change Particle
    lore:
    - <empty>
    - <gray><italic>Click to change particle

dcutscene_particle_loc_modify:
    type: item
    material: compass
    display name: <red><bold>Change Location
    lore:
    - <empty>
    - <gray><italic>Click to change particle location

dcutscene_particle_quantity_modify:
    type: item
    material: ender_chest
    display name: <blue><bold>Change Quantity
    lore:
    - <empty>
    - <gray><italic>Click to change particle quantity

dcutscene_particle_range_modify:
    type: item
    material: lead
    display name: <dark_blue><bold>Visibility Range
    lore:
    - <empty>
    - <gray>How far the particle can be seen at.
    - <empty>
    - <gray><italic>Click to change particle visibility range

dcutscene_particle_repeat_modify:
    type: item
    material: repeater
    display name: <light_purple><bold>Repeat Count
    lore:
    - <empty>
    - <gray>How many times the particle
    - <gray>will play at the location.
    - <empty>
    - <gray><italic>Click to change particle repeat count

dcutscene_particle_repeat_interval_modify:
    type: item
    material: clock
    display name: <dark_purple><bold>Repeat Interval
    lore:
    - <empty>
    - <gray><italic>Click to change particle repeat interval

dcutscene_particle_offset_modify:
    type: item
    material: ladder
    display name: <aqua><bold>Particle Offset
    lore:
    - <empty>
    - <gray><italic>Click to change particle offset

dcutscene_particle_procedure_modify:
    type: item
    material: comparator
    display name: <dark_aqua><bold>Set Procedure Script
    lore:
    - <empty>
    - <gray>Set the procedure script
    - <gray>for the particle location.
    - <blue>Definition: <aqua>loc
    - <gray>See the wiki for example usages.
    - <empty>
    - <gray><italic>Click to set procedure

dcutscene_particle_procedure_defs_modify:
    type: item
    material: writable_book
    display name: <dark_green><bold>Set Procedure Definitions
    lore:
    - <empty>
    - <gray>Set definitions to parse for
    - <gray>the procedure script.
    - <gray>See the wiki for example usages.
    - <empty>
    - <gray><italic>Click to set procedure definitions

dcutscene_particle_special_data_modify:
    type: item
    material: enchanted_book
    display name: <dark_red><bold>Special Data
    lore:
    - <empty>
    - <gray>Some particles can have
    - <gray>special data see wiki for
    - <gray>more information.
    - <empty>
    - <gray><italic>Click to set particle special data

dcutscene_particle_velocity_modify:
    type: item
    material: sculk_sensor
    display name: <gray><bold>Set Particle Velocity
    lore:
    - <empty>
    - <gray>Set a velocity vector for the particle
    - <gray>you can input the vector as 1,5,1 if you'd like.
    - <empty>
    - <gray><italic>Click to set particle velocity

dcutscene_particle_teleport_to:
    type: item
    material: nether_star
    display name: <aqua><bold>Teleport to location
    lore:
    - <empty>
    - <gray><italic>Click to teleport to particle location

dcutscene_particle_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to keyframe
    lore:
    - <empty>
    - <gray>Move the particle animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move particle animator

dcutscene_particle_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the particle animator
    - <gray>and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duration particle animator

dcutscene_particle_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_particle_remove:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Particle
    lore:
    - <empty>
    - <gray><italic>Click to remove particle

##################

##Title ###
dcutscene_send_title:
    type: item
    material: bookshelf
    display name: <dark_aqua><bold>Send a title
    lore:
    - <empty>
    - <gray>Send a title to the player.

dcutscene_title_keyframe:
    type: item
    material: bookshelf
    display name: <dark_aqua><bold>Title

dcutscene_title_title_modify:
    type: item
    material: enchanted_book
    display name: <blue><bold>Set title
    lore:
    - <empty>
    - <gray><italic>Click to set title

dcutscene_title_subtitle_modify:
    type: item
    material: book
    display name: <aqua><bold>Set subtitle
    lore:
    - <empty>
    - <gray><italic>Click to set subtitle

dcutscene_title_duration_modify:
    type: item
    material: clock
    display name: <green><bold>Set Duration
    lore:
    - <empty>
    - <gray>Set the fade in, stay, and fade out duration.
    - <empty>
    - <gray><italic>Click to set title duration

dcutscene_title_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the title animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move title animator

dcutscene_title_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the title animator
    - <gray>and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate title animator

dcutscene_title_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_title_remove_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Title
    lore:
    - <empty>
    - <gray><italic>Click to remove title

###########

## Command ########
dcutscene_play_command:
    type: item
    material: book
    display name: <dark_gray><bold>Play Command
    lore:
    - <empty>
    - <gray>Play a command to the player or console.

dcutscene_command_keyframe:
    type: item
    material: book
    display name: <dark_gray><bold>Command

dcutscene_command_modify:
    type: item
    material: writable_book
    display name: <gold><bold>Set Command
    lore:
    - <empty>
    - <gray><italic>Click to set command

dcutscene_command_execute_as_modify:
    type: item
    material: dropper
    display name: <red><bold>Execute as player/server
    lore:
    - <empty>
    - <gray>Specify whether the command will
    - <gray>be ran as the player or in the console.
    - <empty>
    - <gray><italic>Click to set execute as

dcutscene_command_silent_modify:
    type: item
    material: polished_blackstone_pressure_plate
    display name: <gray><bold>Silent
    lore:
    - <empty>
    - <gray>If true the output of the
    - <gray>command will not show for either
    - <gray>the console or player chat.
    - <empty>
    - <gray><italic>Click to set silent command

dcutscene_command_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the command animator
    - <gray>to a new keyframe.
    - <empty>
    - <gray><italic>Click to move command animator

dcutscene_command_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the command animator
    - <gray>and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate command animator

dcutscene_command_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_command_remove_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Command
    lore:
    - <empty>
    - <gray><italic>Click to remove command

###################

### Message ######
dcutscene_add_msg:
    type: item
    material: oak_sign
    display name: <gold><bold>Send Message
    lore:
    - <empty>
    - <gray>Send a message to the player

dcutscene_message_keyframe:
    type: item
    material: oak_sign
    display name: <gold><bold>Message

dcutscene_message_modify:
    type: item
    material: warped_sign
    display name: <blue><bold>Set Message
    lore:
    - <empty>
    - <gray><italic>Click to set message

dcutscene_message_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the message animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move message animator

dcutscene_message_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the message animator
    - <gray>and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate message animator

dcutscene_message_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_message_remove_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Message
    lore:
    - <empty>
    - <gray><italic>Click to remove message
#################

##Sound
dcutscene_add_sound:
    type: item
    material: jukebox
    display name: <blue><bold>Add Sound
    lore:
    - <empty>
    - <gray>Play a sound to the player.

dcutscene_sound_keyframe:
    type: item
    material: jukebox
    display name: <blue><bold>Sound

dcutscene_sound_modify:
    type: item
    material: jukebox
    display name: <blue><bold>Change Sound
    lore:
    - <empty>
    - <gray><italic>Click to change sound

dcutscene_sound_volume_modify:
    type: item
    material: repeater
    display name: <aqua><bold>Change Volume
    lore:
    - <empty>
    - <gray>Volumes greater than 1.0 will be audible from farther away.
    - <empty>
    - <gray><italic>Click to change sound volume

dcutscene_sound_pitch_modify:
    type: item
    material: lever
    display name: <dark_blue><bold>Change Pitch
    lore:
    - <empty>
    - <gray><italic>Click to change sound pitch

dcutscene_sound_loc_modify:
    type: item
    material: beacon
    display name: <dark_aqua><bold>Set Location
    lore:
    - <empty>
    - <gray>The sound will play at this location.
    - <empty>
    - <gray><italic>Click to change sound location

dcutscene_sound_custom_modify:
    type: item
    material: note_block
    display name: <dark_purple><bold>Custom Sound
    lore:
    - <empty>
    - <gray>If you have a custom sound make sure this is true.
    - <empty>
    - <gray><italic>Click to change custom sound

dcutscene_sound_stop_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Stop Sound
    lore:
    - <empty>
    - <gray>Determine what sound or if all sounds will be stopped in this tick.
    - <empty>
    - <gray><italic>Click to stop sound

dcutscene_sound_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the sound animator to
    - <gray>a new tick.
    - <empty>
    - <gray><italic>Click to move sound animator

dcutscene_sound_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the sound animator
    - <gray>and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate sound animator

dcutscene_sound_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_sound_remove_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Sound
    lore:
    - <empty>
    - <gray><italic>Click to remove sound

## Time ########
dcutscene_send_time:
    type: item
    material: clock
    display name: <dark_green><bold>Set Time
    lore:
    - <empty>
    - <gray>Set the time for the player in the cutscene.

dcutscene_time_keyframe:
    type: item
    material: clock
    display name: <dark_green><bold>Time

dcutscene_time_modify:
    type: item
    material: clock
    display name: <dark_green><bold>Set Time
    lore:
    - <empty>
    - <gray>Specify the time in ticks or any valid duration.
    - <empty>
    - <gray><italic>Click to set time

dcutscene_time_duration_modify:
    type: item
    material: repeater
    display name: <blue><bold>Duration
    lore:
    - <empty>
    - <gray>How long the time will appear
    - <gray>for the player before reverting
    - <gray>back to the original time.
    - <empty>
    - <gray><italic>Click to set time duration

dcutscene_time_freeze_modify:
    type: item
    material: ice
    display name: <aqua><bold>Freeze Time
    lore:
    - <empty>
    - <gray>This locks the player's time.
    - <empty>
    - <gray><italic>Click to set freeze time

dcutscene_time_reset_modify:
    type: item
    material: target
    display name: <gold><bold>Reset Time
    lore:
    - <empty>
    - <gray>If true the time will revert
    - <gray>to the world's time.
    - <empty>
    - <gray><italic>Click to set reset time

dcutscene_time_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the time animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move time animator

dcutscene_time_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the time animator
    - <gray>and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate time animator

dcutscene_time_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_time_remove_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Time
    lore:
    - <empty>
    - <gray><italic>Click to remove time animator
################

## Weather ########
dcutscene_set_weather:
    type: item
    material: lightning_rod
    display name: <white><bold>Set Weather
    lore:
    - <empty>
    - <gray>Set the weather for the player in the cutscene.

dcutscene_weather_keyframe:
    type: item
    material: lightning_rod
    display name: <white><bold>Weather

dcutscene_weather_modify:
    type: item
    material: lightning_rod
    display name: <white><bold>Set Weather
    lore:
    - <empty>
    - <gray><italic>Click to set the weather

dcutscene_weather_duration_modify:
    type: item
    material: clock
    display name: <blue><bold>Weather Duration
    lore:
    - <empty>
    - <gray>How long before the weather reverts
    - <gray>to the original for the player.
    - <empty>
    - <gray><italic>Click to set weather duration

dcutscene_weather_move_to_keyframe:
    type: item
    material: tripwire_hook
    display name: <green><bold>Move to a keyframe
    lore:
    - <empty>
    - <gray>Move the weather animator
    - <gray>to a new tick.
    - <empty>
    - <gray><italic>Click to move weather animator

dcutscene_weather_duplicate:
    type: item
    material: shulker_shell
    display name: <blue><bold>Duplicate
    lore:
    - <empty>
    - <gray>Duplicate the weather animator
    - <gray>and move it to a new tick.
    - <empty>
    - <gray><italic>Click to duplicate weather animator

dcutscene_weather_timespot_play:
    type: item
    material: loom
    display name: <gold><bold>Play from here
    lore:
    - <empty>
    - <gray>Play the cutscene from here.
    - <empty>
    - <gray><italic>Click to play from here

dcutscene_weather_remove_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Weather
    lore:
    - <empty>
    - <gray><italic>Click to remove the weather
###################

## Play Scene ##########
dcutscene_play_scene:
    type: item
    material: scute
    display name: <dark_blue><bold>Play a cutscene
    mechanisms:
        custom_model_data: 10015
    lore:
    - <empty>
    - <gray>Play another cutscene at
    - <gray>this tick. This will immediately
    - <gray>stop the previous scene from
    - <gray>playing as well.

dcutscene_play_scene_change:
    type: item
    material: scute
    display name: <dark_blue><bold>Change cutscene to play
    mechanisms:
        custom_model_data: 10015
    lore:
    - <empty>
    - <gray><italic>Click to change cutscene

dcutscene_play_scene_keyframe:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10015

dcutscene_play_scene_keyframe_contains:
    type: item
    material: scute
    mechanisms:
      custom_model_data: 10016

dcutscene_play_scene_remove:
    type: item
    material: scute
    display name: <red><bold>Remove play cutscene animator
    mechanisms:
        custom_model_data: 10005
    lore:
    - <empty>
    - <gray><italic>Click to remove play cutscene animator

## Stop Point ##########
dcutscene_stop_scene:
    type: item
    material: target
    display name: <red><bold>Stop Scene
    lore:
    - <empty>
    - <gray>Stops the cutscene from
    - <gray>processing any further.

dcutscene_stop_scene_keyframe_item:
    type: item
    material: target
    display name: <red><bold>Stop Scene

dcutscene_stop_point_remove_modify:
    type: item
    material: scute
    mechanisms:
        custom_model_data: 10006
    display name: <red><bold>Remove Stop Point
    lore:
    - <empty>
    - <gray><italic>Click to remove stop point

######################