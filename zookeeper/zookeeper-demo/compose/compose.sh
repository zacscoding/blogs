#!/usr/bin/env bash

SCRIPT_PATH=$( cd "$(dirname "$0")" ; pwd -P )

if [[ ! -e "docker-compose.yaml" ]];then
  echo "docker-compose.yaml not found."
  exit 8
fi

function clean(){
  docker-compose  -f docker-compose.yaml down
  deleteZookeeperMeta
}

function up(){
  deleteZookeeperMeta
  docker-compose up --force-recreate
}

function down(){
  docker-compose down;
}

function stop (){
  docker-compose stop;
}

function start (){
  deleteZookeeperMeta
  docker-compose start;
}

function deleteZookeeperMeta() {
  sudo rm -rf ./temp-zookeeper*
}


for opt in "$@"
do
    case "$opt" in
        up)
            up
            ;;
        down)
            down
            ;;
        stop)
            stop
            ;;
        start)
            start
            ;;
        clean)
            clean
            ;;
        restart)
            down
            clean
            up
            ;;

        *)
            echo $"Usage: $0 {up|down|start|stop|clean|restart}"
            exit 1

esac
done
