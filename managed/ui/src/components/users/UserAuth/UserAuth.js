import React, { useEffect, useState } from 'react';
import clsx from 'clsx';
import { YBLoading } from '../../common/indicators';
import { getPromiseState } from '../../../utils/PromiseUtils';
import { LDAPAuth } from './LDAPAuth';

const TABS = [
  {
    label: 'LDAP Configuration',
    id: 'LDAP',
    Component: LDAPAuth
  }
];

export const UserAuth = (props) => {
  const { fetchRunTimeConfigs, runtimeConfigs, isAdmin } = props;
  const [activeTab, setTab] = useState(TABS[0].id);

  const handleTabSelect = (e, tab) => {
    e.stopPropagation();
    setTab(tab);
  };

  const isTabsVisible = TABS.length > 1; //Now we have only LDAP, OIDC is coming soon
  const currentTab = TABS.find((tab) => tab.id === activeTab);
  const isLoading =
    !runtimeConfigs ||
    getPromiseState(runtimeConfigs).isLoading() ||
    getPromiseState(runtimeConfigs).isInit();
  const Component = currentTab.Component;

  useEffect(() => {
    if (!isAdmin) window.location.href = '/';
    else fetchRunTimeConfigs();
  }, [fetchRunTimeConfigs, isAdmin]);

  return (
    <div className="user-auth-container">
      {isTabsVisible && (
        <>
          <div className="ua-tab-container">
            {TABS.map(({ label, id }) => (
              <div
                key={id}
                className={clsx('ua-tab-item', id === activeTab && 'ua-active-tab')}
                onClick={(e) => handleTabSelect(e, id)}
              >
                {label}
              </div>
            ))}
          </div>
          <div className="ua-sec-divider" />
        </>
      )}

      <div className={clsx('ua-form-container', !isTabsVisible && 'pl-15')}>
        {isLoading ? <YBLoading /> : <Component {...props} isTabsVisible={isTabsVisible} />}
      </div>
    </div>
  );
};
