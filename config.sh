# Resolves links - $0 may be a softlink
cur_dir="${BASH_SOURCE:-$0}"
common_dir="$(cd -P -- "$(dirname -- "$cur_dir")" && pwd -P)"
target="$(basename -- "$cur_dir")"
cur_dir="$common_dir/$target"

# Converts relative path to absolute path
common_dir="`dirname "$cur_dir"`"
common_dir="`cd "$common_dir"; pwd`"
target="`basename "$cur_dir"`"
cur_dir="$common_dir/$target"

export SPECRPC_HOME="`dirname "$cur_dir"`"
echo "SPECRPC_HOME is set to $SPECRPC_HOME"
