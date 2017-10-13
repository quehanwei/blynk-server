import React from 'react';
import WidgetSettings from '../WidgetSettings';
import PropTypes from 'prop-types';

import {fromJS} from 'immutable';

import Validation from 'services/Validation';
import ColorPicker from 'components/ColorPicker';

import _ from 'lodash';

import {
  BAR_CHART_PARAMS
} from 'services/Widgets';

import {
  Row,
  Col,
  Select as AntdSelect
} from 'antd';

import {
  Item,
  ItemsGroup
} from "components/UI";

import {
  MetadataSelect as Select
} from 'components/Form';

import {
  reduxForm,
  Field,
  reset,
  initialize,
  getFormValues,
  change,
} from 'redux-form';

import {
  SimpleContentEditable
} from 'components';

import {
  connect
} from 'react-redux';
import {
  bindActionCreators
} from 'redux';

@connect((state, ownProps) => ({
  formValues: (getFormValues(ownProps.form)(state) || {}),
  dataStreams: (state.Product.edit.dataStreams && state.Product.edit.dataStreams.fields || []),
}), (dispatch) => ({
  initializeForm: bindActionCreators(initialize, dispatch),
  resetForm: bindActionCreators(reset, dispatch),
  changeForm: bindActionCreators(change, dispatch),
}))
@reduxForm()
class BarChartSettings extends React.Component {

  static propTypes = {
    visible: PropTypes.bool,
    pristine: PropTypes.bool,

    form: PropTypes.string,

    onClose: PropTypes.func,
    onSubmit: PropTypes.func,
    resetForm: PropTypes.func,
    changeForm: PropTypes.func,
    handleSubmit: PropTypes.func,
    initializeForm: PropTypes.func,

    initialValues: PropTypes.object,

    formValues: PropTypes.shape({
      dataSource: PropTypes.array, // type|pin|columnType, e.g. dataStream|100|Start Time
      sources: PropTypes.arrayOf(PropTypes.shape({
        sourceType: PropTypes.oneOf(['RAW_DATA', 'SUM', 'AVG', 'MED', 'MIN', 'MAX', 'COUNT']),
        dataStream: PropTypes.shape({
          id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
          pin: PropTypes.number,
          name: PropTypes.string
        }),
        selectedColumns: PropTypes.arrayOf(PropTypes.shape({
          name: PropTypes.string.isRequired,
          label: PropTypes.string.isRequired,
          type: PropTypes.oneOf(['METADATA', 'COLUMN']).isRequired,
        })),
        groupByFields: PropTypes.arrayOf(PropTypes.shape({
          name: PropTypes.string.isRequired,
          type: PropTypes.oneOf(['METADATA', 'COLUMN']).isRequired,
        })),
        sortBy: PropTypes.array,
        groupBy: PropTypes.array,
      })),
    }),

    dataStreams: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.number,
      values: PropTypes.shape({
        pin: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
        label: PropTypes.string,
        tableDescriptor: PropTypes.shape({
          columns: PropTypes.arrayOf(PropTypes.shape({
            columnName: PropTypes.string,
            label: PropTypes.string,
          }))
        })
      })
    }))
  };

  constructor(props) {
    super(props);

    this.handleSave = this.handleSave.bind(this);
    this.handleCancel = this.handleCancel.bind(this);
    this.sortByFieldsSelectComponent = this.sortByFieldsSelectComponent.bind(this);
    this.groupByFieldsSelectComponent = this.groupByFieldsSelectComponent.bind(this);
    this.sourceMultipleSelectComponent = this.sourceMultipleSelectComponent.bind(this);
  }

  componentWillUpdate(nextProps) {

    if (!_.isEqual(nextProps.initialValues, this.props.initialValues)) {
      this.props.initializeForm(nextProps.form, nextProps.initialValues);
    }

    const nextDataStream = nextProps.formValues.sources && nextProps.formValues.sources[0].dataStream || {};
    const dataStream = this.props.formValues.sources && this.props.formValues.sources[0].dataStream || {};

    if (nextProps.formValues.sources[0] && nextDataStream.pin === undefined || nextDataStream.pin !== dataStream.pin) {

      let groupByFields = nextProps.formValues.sources[0].groupByFields;
      let sortByFields = nextProps.formValues.sources[0].sortByFields;

      if(groupByFields && groupByFields.length)
        this.props.changeForm(nextProps.form, 'sources.0.groupByFields', []);

      if(sortByFields && sortByFields.length)
        this.props.changeForm(nextProps.form, 'sources.0.sortByFields', []);

    }

  }

  labelNameComponent({input}) {
    return (
      <SimpleContentEditable maxLength={35}
                             className="modal-window-widget-settings-config-widget-name"
                             value={input.value} o
                             onChange={input.onChange}/>
    );
  }

  colorPickerComponent({input}) {
    return (
      <ColorPicker title="primary color" color={input.value}
                   onChange={input.onChange}/>
    );
  }

  getSelectedStream() {
    if(!this.props.formValues.sources)
      return null;

    return _.find(this.props.dataStreams, (stream) => {
      return String(stream.values.id) === String(this.props.formValues.sources[0].dataStream.id);
    });
  }

  getSelectedStreamColumns() {

    let stream = this.getSelectedStream();

    if(!stream)
      return null;

    return stream.values.tableDescriptor && stream.values.tableDescriptor.columns || [];
  }

  getColumnsMetadata(columns) {

    let metadata = [];

    const metaFields = {};

    columns.filter((column) => !!column.metaFields).forEach((column) => {
      column.metaFields.forEach((field) => {
        if (!metaFields[field.id])
          metaFields[field.id] = field;
      });
    });

    _.forEach(metaFields, (metaField) => {
      metadata.push(metaField);
    });

    return metadata;
  }

  getOptionsForGroupBy() {
    let options = [];
    let columns = this.getSelectedStreamColumns();

    if(Array.isArray(columns)) {
      this.getColumnsMetadata(columns).forEach((metadata) => options.push({
        key: String(metadata.id),
        value: metadata.name,
      }));
    }

    return options;
  }

  getOptionsForXData() {

    const dataStreamsOptions = [];

    this.props.dataStreams.forEach((stream) => {

      if(parseInt(stream.values.pin) !== 100)
        return null;

      dataStreamsOptions.push({
        key: `${stream.values.pin}`,
        value: stream.values.label,
      });

      if (stream.values.tableDescriptor && stream.values.tableDescriptor.columns) {
        stream.values.tableDescriptor.columns.forEach((column) => {
          dataStreamsOptions.push({
            key: `${stream.values.pin}|${column.label}`,
            value: `-- ${column.label}`,
            disabled: true,
          });
        });
      }

    });

    return {
      'Data Streams': dataStreamsOptions
    };
  }

  getOptionsForSortBy() {
    let options = [];

    if(this.props.formValues.sources && Array.isArray(this.props.formValues.sources[0].groupByFields) && this.props.formValues.sources[0].groupByFields.length) {
      // get values from groupBy
      options = this.getOptionsForGroupBy().filter((option) => {
        return this.props.formValues.sources[0].groupByFields.some((groupByOption) => ( String(groupByOption.name) === String(option.value) ));
      });
    } else {
      // get all available values like for groupBy
      options = this.getOptionsForGroupBy();
    }

    return options;
  }

  handleCancel() {
    if (typeof this.props.onClose === 'function')
      this.props.onClose();

    this.props.resetForm(this.props.form);
  }

  handleSave() {
    if(typeof this.props.handleSubmit === 'function')
      this.props.handleSubmit();
  }

  multipleTagsSelect(props) {

    const getOption = (item) => {
      return (
        <AntdSelect.Option value={item.key} key={item.key} disabled={item.disabled || false}>
          {item.value}
        </AntdSelect.Option>
      );
    };

    const getOptions = (list) => {
      return list.map((item) => getOption(item));
    };

    const getGroup = (name, list) => {
      return (
        <AntdSelect.OptGroup key={name}>
          {getOptions(list)}
        </AntdSelect.OptGroup>
      );
    };

    const getGroups = (list) => {
      return Object.keys(list).map((key) => getGroup(key, list[key]));
    };

    const values = props.values || [];

    let optionsList = null;

    if (Array.isArray(values)) {
      optionsList = getOptions(values);
    } else {
      optionsList = getGroups(values);
    }

    return (
      <AntdSelect mode="multiple"
                  onFocus={props.input.onFocus}
                  onBlur={props.input.onBlur}
                  onChange={props.input.onChange}
                  notFoundContent={props.notFoundContent || ''}
                  dropdownMatchSelectWidth={props.dropdownMatchSelectWidth || false}
                  value={props.input.value || []}
                  placeholder={props.placeholder || ''}
                  validate={props.validate || []}
                  style={{width: '100%'}}
                  allowClear={true}>
        {optionsList || null}
      </AntdSelect>
    );

  }

  sourceMultipleSelectComponent(props) {

    const onChange = (value) => {

      const getStreamByPin = (pin) => {
        return _.find(props.dataStreams, (stream) => {
          return parseInt(stream.values.pin) === parseInt(pin);
        });
      };

      const getColumnByLabel = (label = null, columns = []) => {
        return _.find(columns, (column) => String(column.label) === String(label));
      };

      let dataStream = null;
      let selectedColumns = [];

      value.forEach((source) => {

        let [pin, columnLabel] = source.split('|');

        if(!dataStream) {
          dataStream = getStreamByPin(pin);
        }

        if(columnLabel) {

          let column = getColumnByLabel(columnLabel, dataStream.values.tableDescriptor.columns);

          selectedColumns.push({
            name: column.columnName,
            label: column.label,
            type: 'COLUMN',
          });

        }

      });

      if(!dataStream || !dataStream.values) {
        props.input.onChange({});
        props.changeForm(this.props.form, 'sources.0.selectedColumns', []);
        return null;
      }

      props.input.onChange(
        fromJS(dataStream.values).set('tableDescriptor', null).toJS()
      );

      props.changeForm(this.props.form, 'sources.0.selectedColumns', selectedColumns);

    };

    const getValue = () => {

      if(!props.input.value) return [];

      let values = [];

      let pin = this.props.formValues.sources[0].dataStream.pin;

      if(!pin)
        return [];

      (this.props.formValues.sources[0].selectedColumns || []).forEach((column) => {
        values.push(`${pin}|${column.label}`);
      });

      if(!values.length) // display stream as selected only if no columns selected
        values.push(String(pin));

      return values;
    };

    return this.multipleTagsSelect({
      ...props,
      input: {
        ...props.input,
        value: getValue(),
        onChange: onChange,
        onBlur: () => {},
      }
    });
  }

  groupByFieldsSelectComponent(props) {

    const onChange = (value) => {

      let groupByColumns = value.map((id) => {
        let item = _.find(this.getOptionsForGroupBy(), (option) => parseInt(option.key) === parseInt(id));

        return {
          name: item.value,
          type: 'METADATA',
        };

      });

      props.input.onChange(groupByColumns);

    };

    const getValue = () => {

      if(!props.input.value) return [];

      let values = [];

      props.input.value.forEach((field) => {
        this.getOptionsForGroupBy().forEach((item) => {
          if (item.value === field.name)
            values.push(item.key);
        });
      });

      return values;

    };

    return this.multipleTagsSelect({
      ...props,
      input: {
        ...props.input,
        value: getValue(),
        onChange: onChange,
        onBlur: onChange,
      }
    });

  }

  sortByFieldsSelectComponent(props) {

    const onChange = (value) => {

      let groupByColumns = value.map((id) => {
        let item = _.find(this.getOptionsForSortBy(), (option) => parseInt(option.key) === parseInt(id));

        return {
          name: item.value,
          type: 'METADATA',
        };

      });

      props.input.onChange(groupByColumns);

    };

    const getValue = () => {

      if(!props.input.value) return [];

      let values = [];

      props.input.value.forEach((field) => {
        this.getOptionsForSortBy().forEach((item) => {
          if (item.value === field.name)
            values.push(item.key);
        });
      });

      return values;

    };

    return this.multipleTagsSelect({
      ...props,
      input: {
        ...props.input,
        value: getValue(),
        onChange: onChange,
        onBlur: onChange,
      }
    });

  }


  render() {

    const sourcesOptions = this.getOptionsForXData();

    const groupByOptions = this.getOptionsForGroupBy();

    const sortByOptions = this.getOptionsForSortBy();

    return (
      <WidgetSettings
        visible={this.props.visible}
        onSave={this.handleSave}
        onCancel={this.handleCancel}
        isSaveDisabled={this.props.pristine}
        config={(
          <div>
            <div className="modal-window-widget-settings-config-column-header">
              <Field name="label" component={this.labelNameComponent}/>

              <div className="modal-window-widget-settings-config-add-source">
                {/*<Button type="dashed" onClick={this.handleAddSource}>Add source</Button>*/}
              </div>

            </div>

            <div className="modal-window-widget-settings-config-column-bar-configuration">
              <ItemsGroup>
                <Item label="X: Data" offset="large">
                  <Select displayError={false}
                          dropdownMatchSelectWidth={false}
                          name="sources.0.sourceType"
                          values={BAR_CHART_PARAMS.DATA_TYPE.list}
                          placeholder="Choose type"
                          validate={[Validation.Rules.required]}
                          style={{width: '100px'}}/>
                </Item>
                <Item label=" " offset="large" style={{width: '100%'}}>

                  <Field name="sources.0.dataStream"
                         style={{width: '100%'}}
                         placeholder="Choose Source"
                         validate={[Validation.Rules.required]}
                         component={this.sourceMultipleSelectComponent}
                         values={sourcesOptions}
                         dataStreams={this.props.dataStreams}
                         formValues={this.props.formValues}
                         changeForm={this.props.changeForm}
                         form={this.props.form}
                  />

                </Item>
              </ItemsGroup>
              <Item label="Y: Group By" offset="large">

                <Field name="sources.0.groupByFields"
                       placeholder="Group By"
                       component={this.groupByFieldsSelectComponent}
                       dataStreams={this.props.dataStreams}
                       formValues={this.props.formValues}
                       changeForm={this.props.changeForm}
                       form={this.props.form}
                       values={groupByOptions}/>
              </Item>
              <ItemsGroup>
                <Item label="Sort By" offset="large" style={{width: '100%'}}>

                  <Field name="sources.0.sortByFields"
                         placeholder="Sort By"
                         dropdownMatchSelectWidth={false}
                         notFoundContent={'There are no available options'}
                         component={this.sortByFieldsSelectComponent}
                         dataStreams={this.props.dataStreams}
                         formValues={this.props.formValues}
                         changeForm={this.props.changeForm}
                         form={this.props.form}
                         values={sortByOptions}
                  />
                </Item>
                <Item label=" ">
                  <Select displayError={false}
                          dropdownMatchSelectWidth={false}
                          values={BAR_CHART_PARAMS.SORT_BY_ORDER.list}
                          name="sources.0.sortOrder"
                          placeholder="Choose Source"
                          style={{width: '100px'}}
                  />
                </Item>
              </ItemsGroup>
              <Row>
                <Col span={8}>

                  <Item label="Max rows">
                    <Select displayError={false}
                            dropdownMatchSelectWidth={false}
                            values={BAR_CHART_PARAMS.MAX_ROWS.list}
                            name="sources.0.limit"
                            placeholder="Choose Max Rows"
                            style={{width: '100px'}}
                    />
                  </Item>

                </Col>
                <Col span={12}>

                  <Item label="Color">
                    <Field component={this.colorPickerComponent} name="sources.0.color"/>
                  </Item>

                </Col>
              </Row>
            </div>
          </div>
        )}

        preview={(
          <div>Preview</div>
        )}
      />
    );
  }

}

export default BarChartSettings;
