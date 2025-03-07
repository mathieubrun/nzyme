import React from 'react'

class SystemStatusState extends React.Component {
  render () {
    return (
            <li className={this.props.state.active ? '' : 'state-not-active'}>
                {this.props.state.name}
            </li>
    )
  }
}

export default SystemStatusState
