import React, { useContext, useEffect, useState } from 'react';
import { Table, Button, Space, Typography, Checkbox, Modal, Spin, Empty, message, Tag } from 'antd';
import { Link } from 'react-router-dom';
import { CartContext } from '../contexts/CartContext';

const { Title } = Typography;

export default function CartPage() {
  const {
    cart,
    removeFromCart,
    updateCartItemQuantity,
    loading,
    fetchCart,
    checkoutCart
  } = useContext(CartContext);

  const [isModalVisible, setIsModalVisible] = useState(false);
  const [checkoutLoading, setCheckoutLoading] = useState(false);
  const [selectedItems, setSelectedItems] = useState([]);

  useEffect(() => {
    fetchCart();
  }, [fetchCart]);

  const total = selectedItems.reduce(
    (sum, item) => sum + Number(item.book.price) * Number(item.quantity),
    0
  );

  const isItemSelectable = (item) => {
    if (item.book.status === 'OUT_OF_STOCK' || Number(item.book.stock) === 0) {
      return false;
    }

    if (Number(item.quantity) > Number(item.book.stock)) {
      return false;
    }

    return true;
  };

  const toggleSelect = (itemId) => {
    const item = cart.find((cartItem) => cartItem.id === itemId);
    if (!item || !isItemSelectable(item)) {
      return;
    }

    setSelectedItems((prev) => {
      const isSelected = prev.some((selected) => selected.id === itemId);
      return isSelected
        ? prev.filter((selected) => selected.id !== itemId)
        : [...prev, item];
    });
  };

  const toggleSelectAll = () => {
    const selectableItems = cart.filter(isItemSelectable);
    if (selectedItems.length === selectableItems.length) {
      setSelectedItems([]);
    } else {
      setSelectedItems(selectableItems);
    }
  };

  const handleUpdateQuantity = async (itemId, newQuantity) => {
    const item = cart.find((cartItem) => cartItem.id === itemId);
    if (!item) {
      return;
    }

    if (newQuantity > Number(item.book.stock)) {
      message.error(`库存不足，最多只能购买 ${item.book.stock} 本`);
      return;
    }

    try {
      await updateCartItemQuantity(itemId, newQuantity);
      setSelectedItems((prev) =>
        prev.map((selectedItem) =>
          selectedItem.id === itemId ? { ...selectedItem, quantity: newQuantity } : selectedItem
        )
      );
    } catch (err) {
      message.error('更新数量失败');
    }
  };

  const handleRemove = async (itemId) => {
    try {
      await removeFromCart(itemId);
      setSelectedItems((prev) => prev.filter((item) => item.id !== itemId));
      message.success('商品已从购物车移除');
    } catch (err) {
      message.error('删除失败');
    }
  };

  const columns = [
    {
      title: (
        <Checkbox
          checked={
            selectedItems.length === cart.filter(isItemSelectable).length && cart.some(isItemSelectable)
          }
          indeterminate={
            selectedItems.length > 0 && selectedItems.length < cart.filter(isItemSelectable).length
          }
          onChange={toggleSelectAll}
        >
          全选
        </Checkbox>
      ),
      dataIndex: 'selected',
      key: 'selected',
      render: (_, record) => (
        <Checkbox
          checked={selectedItems.some((item) => item.id === record.id)}
          onChange={() => toggleSelect(record.id)}
          disabled={!isItemSelectable(record)}
        />
      )
    },
    {
      title: '图书封面',
      dataIndex: ['book', 'cover'],
      key: 'cover',
      render: (cover, record) => (
        <div style={{ position: 'relative' }}>
          <img
            src={cover}
            alt="图书封面"
            style={{
              width: '60px',
              height: '80px',
              objectFit: 'cover',
              opacity: !isItemSelectable(record) ? 0.5 : 1
            }}
          />
          {!isItemSelectable(record) && (
            <Tag
              color="error"
              style={{
                position: 'absolute',
                top: 0,
                right: 0,
                fontSize: '12px'
              }}
            >
              {Number(record.book.stock) === 0 ? '售罄' : '库存不足'}
            </Tag>
          )}
        </div>
      )
    },
    {
      title: '书名',
      dataIndex: ['book', 'title'],
      key: 'title',
      render: (title, record) => (
        <Link to={`/book/${record.book.id}`} style={{ color: !isItemSelectable(record) ? '#999' : 'inherit' }}>
          {title}
        </Link>
      )
    },
    {
      title: '作者',
      dataIndex: ['book', 'author'],
      key: 'author'
    },
    {
      title: '单价',
      dataIndex: ['book', 'price'],
      key: 'price',
      render: (price) => `￥${price}`
    },
    {
      title: '数量',
      dataIndex: 'quantity',
      key: 'quantity',
      render: (quantity, record) => (
        <Space direction="vertical" size="small">
          <Space>
            <Button size="small" onClick={() => handleUpdateQuantity(record.id, quantity - 1)} disabled={quantity <= 1}>
              -
            </Button>
            <span>{quantity}</span>
            <Button
              size="small"
              onClick={() => handleUpdateQuantity(record.id, quantity + 1)}
              disabled={quantity >= Number(record.book.stock)}
            >
              +
            </Button>
          </Space>
          <div style={{ fontSize: '12px', color: '#666' }}>库存: {record.book.stock ?? '未知'} 本</div>
        </Space>
      )
    },
    {
      title: '小计',
      key: 'subtotal',
      render: (_, record) => `￥${(Number(record.book.price) * Number(record.quantity)).toFixed(2)}`
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button type="link" danger onClick={() => handleRemove(record.id)}>
          删除
        </Button>
      )
    }
  ];

  const handleCheckout = async () => {
    if (selectedItems.length === 0) {
      message.warning('请至少选择一个商品');
      return;
    }

    try {
      setCheckoutLoading(true);
      await checkoutCart(selectedItems);
      setIsModalVisible(true);
      setSelectedItems([]);
      message.success('结算成功');
    } catch (err) {
      message.error(`结算失败: ${err.message || '未知错误'}`);
      console.error('结算失败:', err);
    } finally {
      setCheckoutLoading(false);
    }
  };

  const renderContent = () => {
    if (loading) {
      return (
        <div style={{ textAlign: 'center', margin: '50px 0' }}>
          <Spin size="large" />
        </div>
      );
    }

    if (!cart || cart.length === 0) {
      return <Empty description="购物车为空" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
    }

    return (
      <>
        <Table columns={columns} dataSource={cart} rowKey="id" pagination={false} />
        <div style={{ textAlign: 'right', marginTop: '24px' }}>
          <Space>
            <span>已选择 {selectedItems.length} 件商品</span>
            <Title level={4} style={{ margin: 0 }}>
              总计：￥{total.toFixed(2)}
            </Title>
            <Button
              type="primary"
              size="large"
              onClick={handleCheckout}
              disabled={selectedItems.length === 0}
              loading={checkoutLoading}
            >
              结算（{selectedItems.length}）
            </Button>
          </Space>
        </div>
      </>
    );
  };

  return (
    <div style={{ padding: '24px' }}>
      <Title level={2}>购物车</Title>
      {renderContent()}

      <Modal
        title="购买成功"
        open={isModalVisible}
        onOk={() => setIsModalVisible(false)}
        onCancel={() => setIsModalVisible(false)}
      >
        <p>购买成功，商品已加入订单列表。</p>
      </Modal>
    </div>
  );
}
