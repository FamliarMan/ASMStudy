本工程主要是用来学习如何实现一个支持增量编译的高性能的gradle transform以及后续的asm框架练习

1. 编译
拉取源码后应该先注释掉各个build.gradle中的
```
        classpath 'com.jianglei:asm-plugin:1.0'
```
以及
```
apply plugin: 'com.jianglei.method-tracer'
```
因为这个时候，你还未完成第一次编译，这个插件还未发布到本地maven仓库，所以第一次编译会失败，先注释掉，完成第一次编译

2. 修改本地仓库地址
直接修改根目录下的gradle.properties文件中的LOCAL_MAVEN的地址即可

3. 发布插件到本地仓库
执行 ./gradlew :asmplugin:uploadArchives

4. 反注释第一步的代码
去掉第一步代码的注释，这个时候直接编译就是集成了插件的功能了
