# MiniConnect client

Command line REPL client for miniConnect

## Installation

[See the Releases page for downloadable artifacts](https://github.com/miniconnect/miniconnect-client/releases)

Currently, there are two distributions available for download:

- **fat jar**: standalone all-in-one jar file, can be executed with `java -jar <...>`
- **debian package**: standard package for any Debian based OS,
  installs `micl` and `miniconnect-client` binaries and their accessories

## Usage

You can connect to a miniConnect server with the `micl` (or `miniconnect-client`) command:

```bash
micl
```

In case of custom server and port:

```bash
micl server.local:9876
```
