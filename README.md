# 실전! Querydsl
# 김영한 querydsl 공부 

## Quserydsl 설정
### buildgradle 설정 
``` 
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.1.0'
    id 'io.spring.dependency-management' version '1.1.0'
    //querydsl 추가
    id "com.ewerk.gradle.plugins.querydsl" version "1.0.10"
}
apply plugin: 'io.spring.dependency-management'
apply plugin: "com.ewerk.gradle.plugins.querydsl"

group = 'study'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17' //17.0.3

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // QueryDSL 설정
    implementation "com.querydsl:querydsl-jpa:5.0.0:jakarta"
    annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
    annotationProcessor "jakarta.annotation:jakarta.annotation-api"
    annotationProcessor "jakarta.persistence:jakarta.persistence-api"
    // -- QueryDSL ---

    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'com.h2database:h2'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
/*
 * queryDSL 설정 추가
 */
// Querydsl 설정부
def generated = 'src/main/generated'

querydsl {
    jpa = true
    querydslSourcesDir = generated
}
sourceSets {
    main.java.srcDir generated
}

// querydsl 컴파일시 사용할 옵션 설정
compileQuerydsl{
    options.annotationProcessorPath = configurations.querydsl
}
// querydsl 이 compileClassPath 를 상속하도록 설정
configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
    querydsl.extendsFrom compileClasspath
}
``` 
![image](https://github.com/nodwon/querydsl/assets/73088512/ece4a416-a09d-4e48-8692-c037a899ee99)


```
com.querydsl.apt.jpa.JPAAnnotationProcessor
```
### Querydsl은 JPA를 사용하여 쿼리를 작성하는 도구입니다. Querydsl은 깃에 올리면 안되는 이유.

첫째, Querydsl은 라이브러리이기 때문에 깃에 올리면 다른 개발자들이 Querydsl을 사용할 때 충돌이 발생할 수 있습니다.

둘째, Querydsl은 LGPL 라이선스를 가지고 있기 때문에 깃에 올리면 다른 개발자들이 Querydsl을 사용할 때 라이선스 문제를 겪을 수 있습니다.

셋째, Querydsl은 자주 업데이트되기 때문에 깃에 올리면 다른 개발자들이 Querydsl을 사용할 때 최신 버전을 사용할 수 없을 수 있습니다.
