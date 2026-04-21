import React, { useState } from 'react';
import { Card, DatePicker, Button, Table, message, Tabs, Row, Col, Statistic, Spin } from 'antd';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { statisticsApi } from '../services/api';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { TabPane } = Tabs;

const AdminStatisticsPage = () => {
  const [dateRange, setDateRange] = useState([
    dayjs().subtract(30, 'day'),
    dayjs()
  ]);
  const [bookSalesData, setBookSalesData] = useState([]);
  const [userConsumptionData, setUserConsumptionData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('books');

  // 图表颜色
  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d', '#ffc658', '#ff7c7c'];

  // 加载书籍销量统计
  const loadBookSalesStatistics = async () => {
    if (!dateRange || dateRange.length !== 2) {
      message.warning('请选择时间范围');
      return;
    }

    setLoading(true);
    try {
      const [start, end] = dateRange;
      console.log('查询书籍统计 - 开始时间:', start.format('YYYY-MM-DDTHH:mm:ss'));
      console.log('查询书籍统计 - 结束时间:', end.format('YYYY-MM-DDTHH:mm:ss'));
      
      const data = await statisticsApi.getBookSalesStatistics(
        start.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
        end.endOf('day').format('YYYY-MM-DDTHH:mm:ss')
      );
      console.log('书籍销量统计数据:', data);
      setBookSalesData(data || []);
    } catch (error) {
      console.error('加载书籍销量统计失败:', error);
      message.error('加载书籍销量统计失败: ' + (error.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  // 加载用户消费统计
  const loadUserConsumptionStatistics = async () => {
    if (!dateRange || dateRange.length !== 2) {
      message.warning('请选择时间范围');
      return;
    }

    setLoading(true);
    try {
      const [start, end] = dateRange;
      console.log('查询用户统计 - 开始时间:', start.format('YYYY-MM-DDTHH:mm:ss'));
      console.log('查询用户统计 - 结束时间:', end.format('YYYY-MM-DDTHH:mm:ss'));
      
      const data = await statisticsApi.getUserConsumptionStatistics(
        start.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
        end.endOf('day').format('YYYY-MM-DDTHH:mm:ss')
      );
      console.log('用户消费统计数据:', data);
      setUserConsumptionData(data || []);
    } catch (error) {
      console.error('加载用户消费统计失败:', error);
      message.error('加载用户消费统计失败: ' + (error.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  // 加载统计数据
  const loadStatistics = () => {
    if (activeTab === 'books') {
      loadBookSalesStatistics();
    } else {
      loadUserConsumptionStatistics();
    }
  };

  // 书籍销量表格列
  const bookColumns = [
    {
      title: '排名',
      dataIndex: 'rank',
      key: 'rank',
      width: 80,
      render: (_, __, index) => index + 1,
    },
    {
      title: '书籍封面',
      dataIndex: 'cover',
      key: 'cover',
      width: 100,
      render: (cover) => (
        <img 
          src={cover || '/placeholder-book.jpg'} 
          alt="封面" 
          style={{ width: 60, height: 80, objectFit: 'cover' }}
        />
      ),
    },
    {
      title: '书名',
      dataIndex: 'bookTitle',
      key: 'bookTitle',
      width: 200,
    },
    {
      title: '作者',
      dataIndex: 'author',
      key: 'author',
      width: 150,
    },
    {
      title: '销量',
      dataIndex: 'totalSales',
      key: 'totalSales',
      width: 100,
      render: (sales) => `${sales} 本`,
    },
    {
      title: '销售额',
      dataIndex: 'totalRevenue',
      key: 'totalRevenue',
      width: 120,
      render: (revenue) => `¥${revenue}`,
    },
    {
      title: '平均价格',
      dataIndex: 'averagePrice',
      key: 'averagePrice',
      width: 120,
      render: (price) => `¥${price}`,
    },
  ];

  // 用户消费表格列
  const userColumns = [
    {
      title: '排名',
      dataIndex: 'rank',
      key: 'rank',
      width: 80,
      render: (_, __, index) => index + 1,
    },
    {
      title: '用户名',
      dataIndex: 'userName',
      key: 'userName',
      width: 150,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 200,
    },
    {
      title: '订单数',
      dataIndex: 'totalOrders',
      key: 'totalOrders',
      width: 100,
      render: (orders) => `${orders} 单`,
    },
    {
      title: '购书数量',
      dataIndex: 'totalBooks',
      key: 'totalBooks',
      width: 120,
      render: (books) => `${books} 本`,
    },
    {
      title: '总消费',
      dataIndex: 'totalConsumption',
      key: 'totalConsumption',
      width: 120,
      render: (consumption) => `¥${consumption}`,
    },
    {
      title: '平均订单价值',
      dataIndex: 'averageOrderValue',
      key: 'averageOrderValue',
      width: 140,
      render: (value) => `¥${value}`,
    },
  ];

  // 准备图表数据
  const prepareChartData = () => {
    if (activeTab === 'books') {
      return bookSalesData.slice(0, 10).map(item => ({
        name: item.bookTitle.length > 10 ? item.bookTitle.substring(0, 10) + '...' : item.bookTitle,
        销量: item.totalSales,
        销售额: parseFloat(item.totalRevenue),
      }));
    } else {
      return userConsumptionData.slice(0, 10).map(item => ({
        name: item.userName.length > 8 ? item.userName.substring(0, 8) + '...' : item.userName,
        消费金额: parseFloat(item.totalConsumption),
        订单数: item.totalOrders,
      }));
    }
  };

  // 准备饼图数据
  const preparePieData = () => {
    if (activeTab === 'books') {
      return bookSalesData.slice(0, 8).map(item => ({
        name: item.bookTitle.length > 15 ? item.bookTitle.substring(0, 15) + '...' : item.bookTitle,
        value: item.totalSales,
      }));
    } else {
      return userConsumptionData.slice(0, 8).map(item => ({
        name: item.userName.length > 10 ? item.userName.substring(0, 10) + '...' : item.userName,
        value: parseFloat(item.totalConsumption),
      }));
    }
  };

  const chartData = prepareChartData();
  const pieData = preparePieData();

  // 计算统计汇总
  const getStatsSummary = () => {
    if (activeTab === 'books') {
      const totalSales = bookSalesData.reduce((sum, item) => sum + item.totalSales, 0);
      const totalRevenue = bookSalesData.reduce((sum, item) => sum + parseFloat(item.totalRevenue), 0);
      return {
        totalBooks: bookSalesData.length,
        totalSales,
        totalRevenue,
        avgSalesPerBook: bookSalesData.length > 0 ? (totalSales / bookSalesData.length).toFixed(1) : 0,
      };
    } else {
      const totalUsers = userConsumptionData.length;
      const totalConsumption = userConsumptionData.reduce((sum, item) => sum + parseFloat(item.totalConsumption), 0);
      const totalOrders = userConsumptionData.reduce((sum, item) => sum + item.totalOrders, 0);
      return {
        totalUsers,
        totalConsumption,
        totalOrders,
        avgConsumptionPerUser: totalUsers > 0 ? (totalConsumption / totalUsers).toFixed(2) : 0,
      };
    }
  };

  const stats = getStatsSummary();

  return (
    <div style={{ padding: 24 }}>
      <Card title="数据统计分析" style={{ marginBottom: 24 }}>
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={12}>
            <RangePicker
              value={dateRange}
              onChange={setDateRange}
              format="YYYY-MM-DD"
              placeholder={['开始日期', '结束日期']}
              style={{ width: '100%' }}
            />
          </Col>
          <Col span={12}>
            <Button type="primary" onClick={loadStatistics} loading={loading}>
              查询统计数据
            </Button>
          </Col>
        </Row>

        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="书籍销量统计" key="books">
            {/* 统计概览 */}
            {bookSalesData.length > 0 && (
              <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={6}>
                  <Statistic title="书籍种类" value={stats.totalBooks} suffix="种" />
                </Col>
                <Col span={6}>
                  <Statistic title="总销量" value={stats.totalSales} suffix="本" />
                </Col>
                <Col span={6}>
                  <Statistic title="总销售额" value={stats.totalRevenue} prefix="¥" precision={2} />
                </Col>
                <Col span={6}>
                  <Statistic title="平均销量" value={stats.avgSalesPerBook} suffix="本/种" />
                </Col>
              </Row>
            )}

            {/* 图表展示 */}
            {chartData.length > 0 && (
              <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={14}>
                  <Card title="销量排行榜" size="small">
                    <ResponsiveContainer width="100%" height={300}>
                      <BarChart data={chartData}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="name" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="销量" fill="#8884d8" />
                      </BarChart>
                    </ResponsiveContainer>
                  </Card>
                </Col>
                <Col span={10}>
                  <Card title="销量分布" size="small">
                    <ResponsiveContainer width="100%" height={300}>
                      <PieChart>
                        <Pie
                          data={pieData}
                          cx="50%"
                          cy="50%"
                          labelLine={false}
                          label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                          outerRadius={80}
                          fill="#8884d8"
                          dataKey="value"
                        >
                          {pieData.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip />
                      </PieChart>
                    </ResponsiveContainer>
                  </Card>
                </Col>
              </Row>
            )}

            {/* 详细表格 */}
            <Spin spinning={loading}>
              <Table
                columns={bookColumns}
                dataSource={bookSalesData}
                rowKey="bookId"
                pagination={{ pageSize: 10 }}
                scroll={{ x: 800 }}
              />
            </Spin>
          </TabPane>

          <TabPane tab="用户消费统计" key="users">
            {/* 统计概览 */}
            {userConsumptionData.length > 0 && (
              <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={6}>
                  <Statistic title="消费用户" value={stats.totalUsers} suffix="人" />
                </Col>
                <Col span={6}>
                  <Statistic title="总订单数" value={stats.totalOrders} suffix="单" />
                </Col>
                <Col span={6}>
                  <Statistic title="总消费额" value={stats.totalConsumption} prefix="¥" precision={2} />
                </Col>
                <Col span={6}>
                  <Statistic title="人均消费" value={stats.avgConsumptionPerUser} prefix="¥" />
                </Col>
              </Row>
            )}

            {/* 图表展示 */}
            {chartData.length > 0 && (
              <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={14}>
                  <Card title="消费排行榜" size="small">
                    <ResponsiveContainer width="100%" height={300}>
                      <BarChart data={chartData}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="name" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="消费金额" fill="#82ca9d" />
                      </BarChart>
                    </ResponsiveContainer>
                  </Card>
                </Col>
                <Col span={10}>
                  <Card title="消费分布" size="small">
                    <ResponsiveContainer width="100%" height={300}>
                      <PieChart>
                        <Pie
                          data={pieData}
                          cx="50%"
                          cy="50%"
                          labelLine={false}
                          label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                          outerRadius={80}
                          fill="#8884d8"
                          dataKey="value"
                        >
                          {pieData.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip />
                      </PieChart>
                    </ResponsiveContainer>
                  </Card>
                </Col>
              </Row>
            )}

            {/* 详细表格 */}
            <Spin spinning={loading}>
              <Table
                columns={userColumns}
                dataSource={userConsumptionData}
                rowKey="userId"
                pagination={{ pageSize: 10 }}
                scroll={{ x: 900 }}
              />
            </Spin>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};

export default AdminStatisticsPage; 
