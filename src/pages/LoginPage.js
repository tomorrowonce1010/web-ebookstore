import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Card, Tabs, message, Row, Col, App } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined, HomeOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';

const { TabPane } = Tabs;

const LoginPage = () => {
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('login');
    const navigate = useNavigate();
    const { modal } = App.useApp();

    // 组件挂载时检查是否已登录
    useEffect(() => {
        if (authService.isAuthenticated()) {
            navigate('/', { replace: true });
        }
    }, [navigate]);

    // 处理登录
    const handleLogin = async (values) => {
        console.log('前端开始处理登录，输入值:', values);
        setLoading(true);
        try {
            //调用authService的login方法进行登录操作
            //await暂停函数完成直到promise完成（异步）
            const result = await authService.login({
                username: values.username,
                password: values.password
            });

            console.log('前端收到登录结果:', result);

            if (result.success) {
                console.log('登录成功，准备跳转');
                message.success('登录成功！');
                // 登录成功后导航到首页
                setTimeout(() => {
                    navigate('/', { replace: true });
                }, 1000);
            } else {
                console.log('登录失败，显示错误弹窗，错误信息:', result.message);
                
                // 使用modal.error显示具体的错误信息
                modal.error({
                    title: '登录失败',
                    icon: <ExclamationCircleOutlined />,
                    content: result.message || '登录失败，请重试',
                    okText: '确定',
                    centered: true,
                });
            }
        } catch (error) {
            console.log('登录过程中发生异常:', error);
            // 网络错误也使用modal.error显示
            modal.error({
                title: '网络错误',
                icon: <ExclamationCircleOutlined />,
                content: '网络连接失败，请检查网络后重试',
                okText: '确定',
                centered: true,
            });
            console.error('登录错误:', error);
        } finally {
            setLoading(false);
        }
    };

    // 处理注册
    const handleRegister = async (values) => {
        setLoading(true);
        try {
            const result = await authService.register({
                username: values.username,
                password: values.password,
                confirmPassword: values.confirmPassword,
                name: values.name,
                email: values.email,
                phone: values.phone,
                address: values.address
            });

            if (result.success) {
                message.success('注册成功！');
                // 注册成功后导航到首页
                setTimeout(() => {
                    navigate('/', { replace: true });
                }, 1000);
            } else {
                // 注册失败也使用Modal弹窗显示
                modal.error({
                    title: '注册失败',
                    icon: <ExclamationCircleOutlined />,
                    content: result.message || '注册失败，请重试',
                    okText: '确定',
                    centered: true,
                });
            }
        } catch (error) {
            // 注册网络错误也使用Modal弹窗显示
            modal.error({
                title: '网络错误',
                icon: <ExclamationCircleOutlined />,
                content: '网络连接失败，请检查网络后重试',
                okText: '确定',
                centered: true,
            });
            console.error('注册错误:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px'
        }}>
            <Row justify="center" style={{ width: '100%', maxWidth: '400px' }}>
                <Col xs={24} sm={20} md={16} lg={24}>
                    <Card 
                        style={{ 
                            borderRadius: '10px',
                            boxShadow: '0 4px 20px rgba(0, 0, 0, 0.1)'
                        }}
                        title={
                            <div style={{ textAlign: 'center', color: '#1890ff', fontSize: '24px', fontWeight: 'bold' }}>
                                在线书店
                            </div>
                        }
                    >
                        <Tabs 
                            activeKey={activeTab} 
                            onChange={setActiveTab}
                            centered
                            size="large"
                        >
                            <TabPane tab="登录" key="login">
                                <Form
                                    name="login"
                                    onFinish={handleLogin}
                                    autoComplete="off"
                                    size="large"
                                >
                                    <Form.Item
                                        name="username"
                                        rules={[{ required: true, message: '请输入用户名！' }]}
                                    >
                                        <Input 
                                            prefix={<UserOutlined />} 
                                            placeholder="用户名" 
                                        />
                                    </Form.Item>

                                    <Form.Item
                                        name="password"
                                        rules={[{ required: true, message: '请输入密码！' }]}
                                    >
                                        <Input.Password
                                            prefix={<LockOutlined />}
                                            placeholder="密码"
                                        />
                                    </Form.Item>

                                    <Form.Item>
                                        <Button 
                                            type="primary" 
                                            htmlType="submit" 
                                            loading={loading}
                                            style={{ width: '100%', height: '40px' }}
                                        >
                                            登录
                                        </Button>
                                    </Form.Item>
                                </Form>
                            </TabPane>

                            <TabPane tab="注册" key="register">
                                <Form
                                    name="register"
                                    onFinish={handleRegister}
                                    autoComplete="off"
                                    size="large"
                                >
                                    <Form.Item
                                        name="username"
                                        rules={[
                                            { required: true, message: '请输入用户名！' },
                                            { min: 3, message: '用户名至少3个字符！' }
                                        ]}
                                    >
                                        <Input 
                                            prefix={<UserOutlined />} 
                                            placeholder="用户名" 
                                        />
                                    </Form.Item>

                                    <Form.Item
                                        name="password"
                                        rules={[
                                            { required: true, message: '请输入密码！' },
                                            { min: 6, message: '密码至少6个字符！' }
                                        ]}
                                    >
                                        <Input.Password
                                            prefix={<LockOutlined />}
                                            placeholder="密码"
                                        />
                                    </Form.Item>

                                    <Form.Item
                                        name="confirmPassword"
                                        dependencies={['password']}
                                        rules={[
                                            { required: true, message: '请确认密码！' },
                                            ({ getFieldValue }) => ({
                                                validator(_, value) {
                                                    if (!value || getFieldValue('password') === value) {
                                                        return Promise.resolve();
                                                    }
                                                    return Promise.reject(new Error('两次输入的密码不一致！'));
                                                },
                                            }),
                                        ]}
                                    >
                                        <Input.Password
                                            prefix={<LockOutlined />}
                                            placeholder="确认密码"
                                        />
                                    </Form.Item>

                                    <Form.Item
                                        name="name"
                                        rules={[{ required: true, message: '请输入姓名！' }]}
                                    >
                                        <Input 
                                            prefix={<UserOutlined />} 
                                            placeholder="姓名" 
                                        />
                                    </Form.Item>

                                    <Form.Item
                                        name="email"
                                        rules={[
                                            { required: true, message: '请输入邮箱！' },
                                            { type: 'email', message: '邮箱格式不正确！' }
                                        ]}
                                    >
                                        <Input 
                                            prefix={<MailOutlined />} 
                                            placeholder="邮箱" 
                                        />
                                    </Form.Item>

                                    <Form.Item name="phone">
                                        <Input 
                                            prefix={<PhoneOutlined />} 
                                            placeholder="手机号码（可选）" 
                                        />
                                    </Form.Item>

                                    <Form.Item name="address">
                                        <Input 
                                            prefix={<HomeOutlined />} 
                                            placeholder="地址（可选）" 
                                        />
                                    </Form.Item>

                                    <Form.Item>
                                        <Button 
                                            type="primary" 
                                            htmlType="submit" 
                                            loading={loading}
                                            style={{ width: '100%', height: '40px' }}
                                        >
                                            注册
                                        </Button>
                                    </Form.Item>
                                </Form>
                            </TabPane>
                        </Tabs>
                    </Card>
                </Col>
            </Row>
        </div>
    );
};

export default LoginPage; 
