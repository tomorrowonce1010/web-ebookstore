import React, { useCallback, useEffect, useState } from 'react';
import { Form, Input, Button, Row, Col, Card, message, Spin } from 'antd';
import { userApi } from '../services/api';
import authService from '../services/authService';

export default function PersonalInfo() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [initialValues, setInitialValues] = useState(null);

  const fetchUserInfo = useCallback(async () => {
    try {
      setLoading(true);
      const response = await userApi.getUserInfo();

      if (response && response.success) {
        const userInfo = response.data;
        const values = {
          username: authService.getCurrentUser()?.username || '',
          name: userInfo.name || '',
          email: userInfo.email || '',
          address: userInfo.address || ''
        };

        setInitialValues(values);
        form.setFieldsValue(values);
      } else {
        throw new Error(response.message || '获取用户信息失败');
      }
    } catch (err) {
      console.error('获取用户信息失败:', err);
      message.error('获取用户信息失败，请稍后再试');

      const localUserInfo = authService.getCurrentUser();
      if (localUserInfo) {
        const values = {
          username: localUserInfo.username || '',
          name: localUserInfo.name || '',
          email: localUserInfo.email || '',
          address: localUserInfo.address || ''
        };
        setInitialValues(values);
        form.setFieldsValue(values);
      }
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => {
    fetchUserInfo();
  }, [fetchUserInfo]);

  const onFinish = async (values) => {
    const hasChanges = Object.keys(values).some(
      (key) => key !== 'username' && values[key] !== initialValues?.[key]
    );

    if (!hasChanges) {
      message.info('没有信息被修改');
      return;
    }

    try {
      setSubmitting(true);
      const response = await userApi.updateUserInfo({
        name: values.name,
        email: values.email,
        address: values.address
      });

      if (response && response.success) {
        const currentUser = authService.getCurrentUser();
        if (currentUser) {
          authService.setCurrentUser({
            ...currentUser,
            name: values.name,
            email: values.email,
            address: values.address
          });
        }

        setInitialValues(values);
        message.success('用户信息更新成功');
      } else {
        throw new Error(response.message || '更新用户信息失败');
      }
    } catch (err) {
      console.error('保存用户信息失败:', err);
      message.error(`保存失败: ${err.message || '请稍后再试'}`);
    } finally {
      setSubmitting(false);
    }
  };

  const handleReset = () => {
    if (initialValues) {
      form.setFieldsValue(initialValues);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', margin: '50px 0' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: '24px' }}>
      <h2 style={{ marginBottom: '24px' }}>个人信息</h2>
      <Form
        form={form}
        onFinish={onFinish}
        layout="vertical"
        initialValues={initialValues}
      >
        <Card title="基本信息" bordered={false} style={{ marginBottom: '24px' }}>
          <Row gutter={24}>
            <Col span={12}>
              <Form.Item name="username" label="用户名">
                <Input disabled />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="name"
                label="姓名"
                rules={[{ required: true, message: '请输入姓名' }]}
              >
                <Input placeholder="请输入姓名" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={24}>
            <Col span={12}>
              <Form.Item
                name="email"
                label="邮箱"
                rules={[
                  { required: true, message: '请输入邮箱' },
                  { type: 'email', message: '请输入有效的邮箱' }
                ]}
              >
                <Input placeholder="请输入邮箱" />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        <Card title="地址信息" bordered={false} style={{ marginBottom: '24px' }}>
          <Form.Item
            name="address"
            label="地址"
            rules={[{ required: true, message: '请输入地址' }]}
          >
            <Input placeholder="请输入地址" />
          </Form.Item>
        </Card>

        <Row justify="end" gutter={16}>
          <Col>
            <Button onClick={handleReset}>重置</Button>
          </Col>
          <Col>
            <Button type="primary" htmlType="submit" loading={submitting}>
              保存
            </Button>
          </Col>
        </Row>
      </Form>
    </div>
  );
}
