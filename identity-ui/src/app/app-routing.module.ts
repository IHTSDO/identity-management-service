import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { MainComponent } from './components/main/main.component';
import { LoginComponent } from './components/login/login.component';
import { LogoutComponent } from './components/logout/logout.component';

const routes: Routes = [
    {path: '', component: MainComponent, redirectTo: 'login'},
    {path: 'home', component: MainComponent, redirectTo: 'login'},
    {path: 'login', component: LoginComponent, pathMatch: 'full'},
    {path: 'login?serviceReferer=', component: LoginComponent, pathMatch: 'full'},
    {path: 'logout', component: LogoutComponent, pathMatch: 'full'},
    {path: 'logout?serviceReferer=', component: LogoutComponent, pathMatch: 'full'}
];

@NgModule({
    imports: [RouterModule.forRoot(routes, {useHash: true})],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
