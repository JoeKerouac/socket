#!/usr/bin/env bash

#第一个参数是成功打印，第二个是失败打印
checkAndExit(){
    check $1 $2
    exit $?
}

#第一个参数是成功打印，第二个是失败打印，失败时退出
check(){
    if [[ $? -eq 0 ]]
    then
    echo $1
    return 0
    else
    echo $2
    exit 1
    fi
}

echo "拉取代码"
git reset --hard origin/master
git fetch --all
git pull
check "代码拉取成功" "代码拉取失败"

if [ ! -n "$1" ]
        then
        echo -e "请传入具体操作，操作可以是：help,package,install,deploy\n 操作示例:sh build.sh package"
elif [ "$1" = "help" ]
        then
        echo -e "\n help:帮助文档\n package:打包\n install:安装到本地\n deploy:发布到maven仓库"
elif [ "$1" = "package" ]
        then
        echo "开始打包"
        mvn clean package -Dmaven.test.skip=true
        checkAndExit "打包完成" "打包失败"
elif [ "$1" = "install" ]
        then
        mvn clean install -Dmaven.test.skip=true
        checkAndExit "安装完成" "安装失败"
elif [ "$1" = "deploy" ]
        then
        mvn clean deploy -P javadoc -P release -Dmaven.test.skip=true -Dgpg.passphrase=$gpg_passphrase -Dgpg.keyname=$gpg_keyname
        checkAndExit "发布完成" "发布失败"
else
        echo "传入参数错误，请查看帮助(help)"
fi