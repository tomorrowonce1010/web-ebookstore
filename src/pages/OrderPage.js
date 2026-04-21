import React, { useState, useEffect } from 'react';
import { Table, Empty, Spin, Card, Input, DatePicker, Button, Space, message } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { orderApi } from '../services/api';
import dayjs from 'dayjs';
const { RangePicker } = DatePicker;

export default function OrderPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchParams, setSearchParams] = useState({
    bookName: '',
    dateRange: null
  });

  // 组件加载时获取订单数据
  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const response = await orderApi.getOrders();
      if (response.success) {
        setOrders(response.data || []);
      } else {
        message.error(response.message || '获取订单失败');
      }
    } catch (error) {
      console.error('获取订单失败:', error);
      message.error('获取订单失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    setLoading(true);
    try {
      const params = {};
      
      if (searchParams.bookName.trim()) {
        params.bookName = searchParams.bookName.trim();
      }
      
      if (searchParams.dateRange && searchParams.dateRange.length === 2) {
        params.startDate = searchParams.dateRange[0].format('YYYY-MM-DDTHH:mm:ss');
        params.endDate = searchParams.dateRange[1].format('YYYY-MM-DDTHH:mm:ss');
      }

      const response = await orderApi.searchOrders(params);
      if (response.success) {
        setOrders(response.data || []);
        message.success(`找到 ${response.data?.length || 0} 条订单记录`);
      } else {
        message.error(response.message || '搜索订单失败');
      }
    } catch (error) {
      console.error('搜索订单失败:', error);
      message.error('搜索订单失败');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setSearchParams({
      bookName: '',
      dateRange: null
    });
    fetchOrders();
  };

  const expandedRowRender = (record) => {
    const columns = [
      { title: '书籍名称', dataIndex: 'title', key: 'title' },
      { title: '作者', dataIndex: 'author', key: 'author' },
      { title: '单价', dataIndex: 'price', key: 'price', render: price => `¥${price}` },
      { title: '数量', dataIndex: 'quantity', key: 'quantity' },
      { title: '小计', dataIndex: 'subtotal', key: 'subtotal', render: subtotal => `¥${subtotal}` },
    ];

    return (
      <Table
        columns={columns}
        dataSource={record.orderItems}
        pagination={false}
        rowKey="id"
        size="small"
      />
    );
  };

  const columns = [
    {
      title: '订单ID',
      dataIndex: 'id',
      key: 'id',
      width: 100,
    },
    {
      title: '订单日期',
      dataIndex: 'orderDate',
      key: 'orderDate',
      width: 180,
      render: (date) => {
        if (!date) return '-';
        return dayjs(date).format('YYYY-MM-DD HH:mm:ss');
      }
    },
    {
      title: '订单总额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      render: (amount) => `¥${amount || 0}`,
    },
    {
      title: '订单状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const statusMap = {
          'PENDING': '待处理',
          'PAID': '已支付',
          'SHIPPED': '已发货',
          'DELIVERED': '已送达',
          'CANCELLED': '已取消',
          'COMPLETED': '已完成'
        };
        return statusMap[status] || status;
      }
    },
    {
      title: '商品数量',
      key: 'itemCount',
      width: 100,
      render: (_, record) => `${record.orderItems?.length || 0} 种商品`,
    },
  ];

  const renderContent = () => {
    if (loading) {
      return (
        <div style={{ textAlign: 'center', margin: '50px 0' }}>
          <Spin size="large" />
        </div>
      );
    }

    if (!orders || orders.length === 0) {
      return (
        <Empty
          description="暂无订单记录"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      );
    }

    return (
      <Table
        columns={columns}
        dataSource={orders}
        rowKey="id"
        expandable={{
          expandedRowRender,
          rowExpandable: (record) => record.orderItems && record.orderItems.length > 0,
        }}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条记录`,
          pageSizeOptions: ['10', '20', '50'],
        }}
        scroll={{ x: 800 }}
      />
    );
  };

  return (
    <div style={{ padding: '24px' }}>
      <Card title="我的订单" style={{ marginBottom: 24 }}>
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Space wrap>
            <Input
              placeholder="输入书籍名称搜索"
              value={searchParams.bookName}
              onChange={(e) => setSearchParams({ ...searchParams, bookName: e.target.value })}
              style={{ width: 200 }}
              allowClear
            />
            <RangePicker
              value={searchParams.dateRange}
              onChange={(dates) => setSearchParams({ ...searchParams, dateRange: dates })}
              showTime
              placeholder={['开始时间', '结束时间']}
            />
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={handleSearch}
              loading={loading}
            >
              搜索
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={handleReset}
              loading={loading}
            >
              重置
            </Button>
          </Space>
          {renderContent()}
        </Space>
      </Card>
    </div>
  );
}


