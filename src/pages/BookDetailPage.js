import React, { useCallback, useContext, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Row, Col, Button, InputNumber, Descriptions, Spin, Alert, message, Tag } from 'antd';
import { CartContext } from '../contexts/CartContext';
import { bookApi } from '../services/api';

export default function BookDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToCart, directPurchase } = useContext(CartContext);

  const [book, setBook] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchBookDetail = useCallback(async () => {
    try {
      setLoading(true);
      const response = await bookApi.getBook(id);

      if (response && response.success) {
        setBook(response.data);
        setError(null);
        return;
      }

      throw new Error(response.message || '获取图书详情失败');
    } catch (err) {
      console.error('获取图书详情失败:', err);
      setError('获取图书详情失败，请稍后再试');

      const { books } = await import('../data');
      const foundBook = books.find((item) => item.id === Number(id));

      if (foundBook) {
        setBook(foundBook);
      } else {
        setError('未找到该图书');
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchBookDetail();
  }, [fetchBookDetail]);

  const handleBuyNow = async () => {
    try {
      setLoading(true);
      await directPurchase(book, quantity);
      message.success('购买成功');
    } catch (err) {
      console.error('购买失败:', err);
      message.error(err.message || '购买失败，请稍后再试');
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = async () => {
    try {
      setLoading(true);
      await addToCart(book, quantity);
      message.success('已添加到购物车');
    } catch (err) {
      console.error('添加到购物车失败:', err);
      message.error(err.message || '添加到购物车失败，请稍后再试');
    } finally {
      setLoading(false);
    }
  };

  const handleBackToHome = () => {
    navigate('/');
  };

  const isOutOfStock = book?.status === 'OUT_OF_STOCK' || Number(book?.stock) === 0;

  if (loading && !book) {
    return (
      <div style={{ textAlign: 'center', margin: '100px 0' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error && !book) {
    return (
      <div style={{ padding: '24px' }}>
        <Alert
          message="错误"
          description={error}
          type="error"
          showIcon
          action={
            <Button type="primary" onClick={handleBackToHome}>
              返回首页
            </Button>
          }
        />
      </div>
    );
  }

  if (!book) {
    return null;
  }

  return (
    <div style={{ padding: '24px' }}>
      <Row gutter={24}>
        <Col span={12}>
          <img
            src={book.cover}
            alt={book.title}
            style={{
              width: '100%',
              maxHeight: '500px',
              objectFit: 'contain',
              border: '1px solid #f0f0f0',
              opacity: isOutOfStock ? 0.5 : 1
            }}
          />
          {isOutOfStock && (
            <Tag
              color="error"
              style={{
                position: 'absolute',
                top: '10px',
                right: '10px',
                fontSize: '14px'
              }}
            >
              售罄
            </Tag>
          )}
        </Col>
        <Col span={12}>
          <h1
            style={{
              fontSize: '24px',
              marginBottom: '16px',
              color: isOutOfStock ? '#999' : 'inherit'
            }}
          >
            {book.title}
          </h1>
          <p
            style={{
              color: isOutOfStock ? '#999' : '#666',
              marginBottom: '24px'
            }}
          >
            {book.description}
          </p>

          <Descriptions bordered column={1}>
            <Descriptions.Item label="作者">{book.author}</Descriptions.Item>
            <Descriptions.Item label="价格">￥{book.price}</Descriptions.Item>
            <Descriptions.Item label="库存数量">
              {Number(book.stock) > 0 ? (
                <span style={{ color: Number(book.stock) <= 10 ? '#ff7875' : '#52c41a' }}>
                  {book.stock} 本
                  {Number(book.stock) <= 10 ? '（库存较少）' : ''}
                </span>
              ) : (
                <Tag color="error">售罄</Tag>
              )}
            </Descriptions.Item>
            {book.isbn && <Descriptions.Item label="ISBN">{book.isbn}</Descriptions.Item>}
          </Descriptions>

          <div style={{ margin: '24px 0' }}>
            <span style={{ marginRight: '16px' }}>数量：</span>
            <InputNumber
              min={1}
              max={Number(book.stock) || 1}
              value={quantity}
              onChange={(value) => setQuantity(value || 1)}
              style={{ width: '100px' }}
              disabled={isOutOfStock}
            />
            {Number(book.stock) > 0 && (
              <span style={{ marginLeft: '8px', color: '#666', fontSize: '12px' }}>
                （最多可购买 {book.stock} 本）
              </span>
            )}
          </div>

          <div>
            <Button
              type="primary"
              size="large"
              style={{ marginRight: '16px' }}
              onClick={handleAddToCart}
              loading={loading}
              disabled={isOutOfStock}
            >
              加入购物车
            </Button>
            <Button
              type="primary"
              size="large"
              danger
              onClick={handleBuyNow}
              loading={loading}
              disabled={isOutOfStock}
            >
              立即购买
            </Button>
          </div>
        </Col>
      </Row>
    </div>
  );
}
