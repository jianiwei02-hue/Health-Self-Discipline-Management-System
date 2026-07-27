# 基于 Spring Boot + Vue 的健康自律管理系统
Health Self Discipline Management System

## 📌 项目介绍

本项目是一款面向大学生群体的健康自律管理系统，采用 **Spring Boot + Vue3 前后端分离架构** 开发。

系统围绕用户健康管理需求，实现个人健康数据记录、饮食管理、运动管理、习惯打卡以及健康趋势分析等功能。

项目通过 **JWT 实现用户身份认证**，使用 **ECharts 实现健康数据可视化展示**，并集成 **百度 AI 菜品识别接口** 辅助用户进行饮食信息录入，提高健康管理效率。

---

## 🏗 系统架构

Vue3 + Element Plus
|
Axios
|
RESTful API
|
Spring Boot + MyBatis
|
MySQL

## 🛠 技术栈

### 前端

- Vue3
- Element Plus
- Axios
- Vue Router
- ECharts
- Vite

### 后端

- Spring Boot
- MyBatis
- MySQL
- JWT
- BCrypt

  ### 第三方服务

- 百度 AI 菜品识别接口

### 开发环境

- JDK 1.8
- MySQL 8.0
- IntelliJ IDEA

---

## ✨ 功能模块

### 用户端

- 用户注册与登录
- 个人信息管理
- 健康数据记录
- 饮食记录
- 运动记录
- 睡眠记录
- BMI计算与健康分析
- 习惯任务打卡
- 健康文章浏览
- 健康社区交流


### 管理端

- 用户管理
- 健康文章管理
- 系统数据维护

---

## 🌟 项目特色

- 📊 健康数据可视化分析
- 🏃 运动与饮食记录管理
- ✅ 习惯打卡激励机制
- 📈 BMI及健康趋势分析
- 🤖 集成百度AI菜品识别接口辅助饮食记录

---

## 📂 项目结构

health-system

├── health-backend
│ └── Spring Boot 后端代码

├── health-frontend
│ └── Vue 前端代码

└── database.sql
└── 数据库文件

---

## 📷 项目截图

### 登录页面
<img width="959" height="504" alt="image" src="https://github.com/user-attachments/assets/3a307348-0400-45d5-98c6-25fdd3e52c01" />


### 首页 Dashboard

![首页](images/home.png)


### 健康数据分析

![健康分析](images/health-data.png)


### 饮食记录

![饮食记录](images/food-record.png)


### 管理后台

![管理后台](images/admin.png)

---

## 🚀 项目运行

### 后端启动

1. 修改 application.yml 数据库配置

2. 启动 Spring Boot 项目


### 前端启动

安装依赖：
npm install

运行：
npm run dev

---

## Future Plan

如果继续迭代：

1. 接入大语言模型生成个性化健康建议
2. 增加 AI 健身助手 Agent
3. 根据用户行为生成长期健康计划
4. 支持多模态健康数据分析

## 👩‍💻 Author

Jiani Wei

Computer Science Graduate

