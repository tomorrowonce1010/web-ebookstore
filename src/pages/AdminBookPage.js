import React, { useState, useEffect } from 'react';
import { 
    Table, 
    Button, 
    Modal, 
    Form, 
    Input, 
    InputNumber, 
    message, 
    Popconfirm, 
    Space, 
    Card,
    Tag 
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, StockOutlined } from '@ant-design/icons';
import { adminBookApi } from '../services/api';
import API_BASE_URL from '../services/config';

const AdminBookPage = () => {
    const [books, setBooks] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editingBook, setEditingBook] = useState(null);
    const [form] = Form.useForm();
    const [searchKeyword, setSearchKeyword] = useState('');
    const [stockModalVisible, setStockModalVisible] = useState(false);
    const [stockForm] = Form.useForm();
    const [editingStock, setEditingStock] = useState(null);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: 10,
        total: 0
    });

    // 获取书籍列表
    const fetchBooks = async (page = 1, pageSize = 10) => {
        setLoading(true);
        try {
            const response = await fetch(`${API_BASE_URL}/admin/books?page=${page - 1}&size=${pageSize}`, {
                credentials: 'include'
            });
            const result = await response.json();
            
            if (result.success) {
                setBooks(result.data);
                setPagination(prev => ({
                    ...prev,
                    current: page,
                    pageSize: pageSize,
                    total: result.total
                }));
            } else {
                message.error(result.message || '获取书籍列表失败');
            }
        } catch (error) {
            console.error('获取书籍列表失败:', error);
            message.error('网络错误，请稍后重试');
        } finally {
            setLoading(false);
        }
    };

    // 搜索书籍
    const searchBooks = async (keyword) => {
        if (!keyword) {
            fetchBooks();
            return;
        }
        
        setLoading(true);
        try {
            const response = await fetch(`${API_BASE_URL}/admin/books/search?keyword=${encodeURIComponent(keyword)}`, {
                credentials: 'include'
            });
            const result = await response.json();
            
            if (result.success) {
                setBooks(result.data);
                setPagination(prev => ({
                    ...prev,
                    total: result.data.length
                }));
            } else {
                message.error(result.message || '搜索失败');
            }
        } catch (error) {
            console.error('搜索失败:', error);
            message.error('网络错误，请稍后重试');
        } finally {
            setLoading(false);
        }
    };

    // 处理分页变化
    const handleTableChange = (newPagination, filters, sorter) => {
        fetchBooks(newPagination.current, newPagination.pageSize);
    };

    // 添加或更新书籍
    const handleSaveBook = async (values) => {
        setLoading(true);
        try {
            const url = editingBook ? 
                `${API_BASE_URL}/admin/books/${editingBook.id}` : 
                `${API_BASE_URL}/admin/books`;
            
            const method = editingBook ? 'PUT' : 'POST';
            
            const response = await fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(values)
            });
            
            const result = await response.json();
            
            if (result.success) {
                message.success(editingBook ? '书籍更新成功' : '书籍添加成功');
                setModalVisible(false);
                setEditingBook(null);
                form.resetFields();
                fetchBooks();
            } else {
                message.error(result.message || '操作失败');
            }
        } catch (error) {
            console.error('保存书籍失败:', error);
            message.error('网络错误，请稍后重试');
        } finally {
            setLoading(false);
        }
    };

    // 删除书籍
    const handleDeleteBook = async (id) => {
        setLoading(true);
        try {
            const response = await fetch(`${API_BASE_URL}/admin/books/${id}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            
            const result = await response.json();
            
            if (result.success) {
                message.success('书籍删除成功');
                fetchBooks();
            } else {
                message.error(result.message || '删除失败');
            }
        } catch (error) {
            console.error('删除书籍失败:', error);
            message.error('网络错误，请稍后重试');
        } finally {
            setLoading(false);
        }
    };

    // 打开编辑模态框
    const handleEdit = (book) => {
        setEditingBook(book);
        form.setFieldsValue(book);
        setModalVisible(true);
    };

    // 打开添加模态框
    const handleAdd = () => {
        setEditingBook(null);
        form.resetFields();
        setModalVisible(true);
    };

    // 关闭模态框
    const handleCancel = () => {
        setModalVisible(false);
        setEditingBook(null);
        form.resetFields();
    };

    // 打开库存管理模态框
    const handleStockEdit = (book) => {
        setEditingStock(book);
        stockForm.setFieldsValue({ stock: book.stock });
        setStockModalVisible(true);
    };

    // 更新库存
    const handleUpdateStock = async (values) => {
        setLoading(true);
        try {
            const response = await adminBookApi.updateStock(editingStock.id, values.stock);
            
            if (response.success) {
                message.success('库存更新成功');
                setStockModalVisible(false);
                setEditingStock(null);
                stockForm.resetFields();
                fetchBooks();
            } else {
                message.error(response.message || '库存更新失败');
            }
        } catch (error) {
            console.error('更新库存失败:', error);
            message.error('网络错误，请稍后重试');
        } finally {
            setLoading(false);
        }
    };

    // 关闭库存模态框
    const handleStockCancel = () => {
        setStockModalVisible(false);
        setEditingStock(null);
        stockForm.resetFields();
    };

    useEffect(() => {
        fetchBooks();
    }, []);

    const columns = [
        {
            title: 'ID',
            dataIndex: 'id',
            key: 'id',
            width: 80,
        },
        {
            title: '书籍标题',
            dataIndex: 'title',
            key: 'title',
        },
        {
            title: '作者',
            dataIndex: 'author',
            key: 'author',
        },
        {
            title: '价格',
            dataIndex: 'price',
            key: 'price',
            render: (price) => `¥${price}`,
        },
        {
            title: '库存',
            dataIndex: 'stock',
            key: 'stock',
            render: (stock, record) => (
                <span style={{ color: stock === 0 ? '#ff4d4f' : stock <= 10 ? '#fa8c16' : '#52c41a' }}>
                    {stock}本
                </span>
            ),
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status, record) => {
                if (record.deleted) {
                    return <Tag color="error">已删除</Tag>;
                }
                if (record.stock === 0) return <Tag color="warning">售罄</Tag>;
                return status === 'AVAILABLE' ? <Tag color="success">有库存</Tag> : <Tag color="default">缺货</Tag>;
            },
        },
        {
            title: 'ISBN',
            dataIndex: 'isbn',
            key: 'isbn',
            render: (isbn) => isbn || '-',
        },
        {
            title: '操作',
            key: 'action',
            width: 250,
            render: (_, record) => (
                <Space size="small">
                    <Button 
                        type="primary" 
                        size="small" 
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                    >
                        编辑
                    </Button>
                    <Button 
                        type="default" 
                        size="small" 
                        icon={<StockOutlined />}
                        onClick={() => handleStockEdit(record)}
                        disabled={record.deleted}
                    >
                        库存
                    </Button>
                    <Popconfirm
                        title={record.deleted ? "确定要恢复这本书吗？" : "确定要删除这本书吗？删除后用户将无法看到和购买此书籍，但历史订单数据会保留。"}
                        onConfirm={() => handleDeleteBook(record.id)}
                        okText="确定"
                        cancelText="取消"
                    >
                        <Button 
                            type="primary" 
                            danger={!record.deleted}
                            size="small" 
                            icon={<DeleteOutlined />}
                            disabled={false}
                        >
                            {record.deleted ? '恢复' : '删除'}
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    return (
        <Card>
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
                <div>
                    <h2>书籍管理</h2>
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                    <Input.Search
                        placeholder="搜索书籍标题或作者"
                        style={{ width: 300 }}
                        value={searchKeyword}
                        onChange={(e) => setSearchKeyword(e.target.value)}
                        onSearch={searchBooks}
                        enterButton={<SearchOutlined />}
                        allowClear
                    />
                    <Button 
                        type="primary" 
                        icon={<PlusOutlined />}
                        onClick={handleAdd}
                    >
                        添加书籍
                    </Button>
                </div>
            </div>

            <Table
                columns={columns}
                dataSource={books}
                rowKey="id"
                loading={loading}
                pagination={{
                    ...pagination,
                    showSizeChanger: true,
                    showQuickJumper: true,
                    showTotal: (total) => `共 ${total} 本书籍`,
                }}
                onChange={handleTableChange}
            />

            <Modal
                title={editingBook ? '编辑书籍' : '添加书籍'}
                visible={modalVisible}
                onCancel={handleCancel}
                onOk={() => form.submit()}
                confirmLoading={loading}
                width={600}
            >
                <Form
                    form={form}
                    layout="vertical"
                    onFinish={handleSaveBook}
                >
                    <Form.Item
                        name="title"
                        label="书籍标题"
                        rules={[{ required: true, message: '请输入书籍标题！' }]}
                    >
                        <Input placeholder="请输入书籍标题" />
                    </Form.Item>

                    <Form.Item
                        name="author"
                        label="作者"
                        rules={[{ required: true, message: '请输入作者！' }]}
                    >
                        <Input placeholder="请输入作者" />
                    </Form.Item>

                    <Form.Item
                        name="price"
                        label="价格"
                        rules={[{ required: true, message: '请输入价格！' }]}
                    >
                        <InputNumber
                            placeholder="请输入价格"
                            style={{ width: '100%' }}
                            min={0}
                            precision={2}
                            formatter={value => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                            parser={value => value.replace(/¥\s?|(,*)/g, '')}
                        />
                    </Form.Item>

                    <Form.Item
                        name="description"
                        label="书籍描述"
                    >
                        <Input.TextArea 
                            placeholder="请输入书籍描述"
                            rows={4}
                        />
                    </Form.Item>

                    <Form.Item
                        name="cover"
                        label="封面图片URL"
                    >
                        <Input placeholder="请输入封面图片URL（可选）" />
                    </Form.Item>

                    <Form.Item
                        name="stock"
                        label="库存数量"
                        rules={[{ required: true, message: '请输入库存数量！' }]}
                        initialValue={100}
                    >
                        <InputNumber
                            placeholder="请输入库存数量"
                            style={{ width: '100%' }}
                            min={0}
                        />
                    </Form.Item>

                    <Form.Item
                        name="isbn"
                        label="ISBN编号"
                    >
                        <Input placeholder="请输入ISBN编号（可选）" />
                    </Form.Item>

                    <Form.Item
                        name="status"
                        label="状态"
                        initialValue="AVAILABLE"
                    >
                        <Input placeholder="状态：AVAILABLE 或 OUT_OF_STOCK" />
                    </Form.Item>
                </Form>
            </Modal>

            {/* 库存管理模态框 */}
            <Modal
                title={`管理库存 - ${editingStock?.title}`}
                visible={stockModalVisible}
                onCancel={handleStockCancel}
                onOk={() => stockForm.submit()}
                confirmLoading={loading}
                width={400}
            >
                <Form
                    form={stockForm}
                    layout="vertical"
                    onFinish={handleUpdateStock}
                >
                    <Form.Item
                        name="stock"
                        label="库存数量"
                        rules={[
                            { required: true, message: '请输入库存数量！' },
                            { type: 'number', min: 0, message: '库存数量不能为负数！' }
                        ]}
                    >
                        <InputNumber
                            placeholder="请输入库存数量"
                            style={{ width: '100%' }}
                            min={0}
                            precision={0}
                        />
                    </Form.Item>
                    <div style={{ color: '#666', fontSize: '12px' }}>
                        当前库存：{editingStock?.stock}本
                    </div>
                </Form>
            </Modal>
        </Card>
    );
};

export default AdminBookPage; 
