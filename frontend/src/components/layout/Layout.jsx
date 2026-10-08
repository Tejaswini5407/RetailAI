import React from 'react';
import { Outlet } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';

const Layout = () => {
  return (
    <div className="app-layout flex-column screen-height">
      <Header />
      <div className="flex-1 flex-row overflow-hidden">
        <Sidebar />
        <main className="app-main-content flex-1 p-4 overflow-y-auto bg-gray-50">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
