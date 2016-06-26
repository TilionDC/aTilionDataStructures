# aTilionDatastructures

### Version 0.1

This plugin adds the functionality of making the bukkit metadata 
persistent throughout a server restart by saving it to a database.
The server owner has the option to what metadata should be persistent.

No permissions or commands implemented yet.

## Features:

 - **SQLite support** - Option to save data to a local SQL database.
 - **Uses the metadata api** - Uses the bukkit metadata api so no 
 dependencies are required to start developing a plugin supporting 
 persistent data.
 - **Easy to use** - Just use the metadata api to save data automaticly.
 - **Support for metadata prefixes** - Save the metadata with 
 ex. Persistent:[meta key]('persistent' is the default metadata prefix)
 to allow it to be saved to the database.
 
 
## Config: 

```
#
#
# Author TilionDC
# @ tiliondc@gmail.com
#
#
# ------------------------------------------------------------------------------ #
#
# Materials can be found at:
# https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html
#
# REMEMBER: When you update the plugin your configuration will update autmaticly which may result in a total
# reset of your configuration. Be careful, the old config will be saved
# in a file marked with the name of the previous version
#
# Version of config. Do NOT change this
Version: 0.0.1

# SQLite config
SQL:
  # If you want to use sqlite as a database for your server.
  Enabled: true
  # The filepath of where the database file should be saved.
  Filepath: '/plugins/ATilionDatastructures/'
  # The filename of your database
  Filename: 'atd.db'
  # Interval time in ticks between every autosave. Set this to 0 if you want it to never autosave
  # X minutes is 20 * 60 * x (change x to amount of minutes). 1 minute = 1200, 5 minutes = 6000, 10 minutes = 12000 etc.
  Saveinterval: 12000
  # If you want the meta data prefix and specific to be case sensitive or not.
  Casesensitive: false
  # A list of all prefixes for metadata that will be saved to the database.
  # If a metadata starts with the name persistent:[name] and 'persistent' is active in this list then all metadata with
  # such a prefix will be added to the database.
  Prefix:
    - 'persistent'
  # Specific metadata that you want to save in a database.
  Specific:
    - 'nick'

```
