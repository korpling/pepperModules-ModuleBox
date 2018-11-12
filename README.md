# Pepper
Pepper is a highly extensible plattform for conversion and manipulation of linguistic data between an unbound set of formats. Pepper can be used stand-alone as a command line interface, or be integrated as an API into other software products.

Pepper can be extended with modules like this one.
For more information how to use Pepper, see the [user guide](http://corpus-tools.org/pepper/userGuide.html).

# ModuleBox

This repository contains several smaller general purpose Pepper modules.

# ScriptManipulator

A module to execute any script from the local file system on the 
document graph of each document. The script gets the document graph in the 
specified format via standard input, must map the document graph and write
the result in the same format to the standard output.

The script will also get two environment variables set:
- `PEPPER_DOCUMENT_NAME` is the name of the document
- `PEPPER_FORMAT` is the name of the format that was configured via the `format` property.

## Properties

| Name of property | Type of property | optional/ mandatory | default value |
| ---------------- | ---------------- | ------------------- | ------------- |
| path             | String           | mandatory           | --            |
| format           | String           | optional            | graphml       |
| args             | String           | optional            | --            |

### path

The path to the script file to execute. If this is a relative path, it must be relative to the
workflow file

### format

The format used to write and read from the script. Can be either "graphml" or "saltxml".

### args

Additional arguments given to the script file.
