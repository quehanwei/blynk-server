import React from 'react';
import {Link} from 'react-router';
import './styles.less';

export default class ProductItem extends React.Component {

  static propTypes = {
    item: React.PropTypes.object
  };

  render() {
    const item =  this.props.item;

    return (
      <div className="product-item">
        <Link to={`/product/${item.id}`}>
          <div className="preview">
            <img src={item.logoUrl}/>
          </div>
          <div className="details">
            <div className="name">
              { item.name }
            </div>
            <div className="amount">
              { 0 } Devices
            </div>
          </div>
        </Link>
      </div>
    );
  }
}
