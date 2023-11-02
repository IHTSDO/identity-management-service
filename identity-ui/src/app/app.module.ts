import { BrowserModule } from '@angular/platform-browser';
import { NgModule, APP_INITIALIZER } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { MainComponent } from './components/main/main.component';
import { LoginComponent } from './components/login/login.component';
import { HttpConfigInterceptor } from './configs/httpconfig.interceptor';
import { PrincipleService } from './services/principle/principle.service';
import { LogoutComponent } from './components/logout/logout.component';
import {SnomedNavbarComponent} from "./components/snomed-navbar/snomed-navbar.component";
import {AuthenticationService} from "./services/authentication/authentication.service";
import {ToastrModule} from "ngx-toastr";
import {SnomedFooterComponent} from "./components/snomed-footer/snomed-footer.component";

export function startupServiceFactory(principle: PrincipleService): Function {
    return () => principle.identity(true);
}

@NgModule({
    declarations: [
        AppComponent,
        MainComponent,
        LoginComponent,
        LogoutComponent,
        SnomedNavbarComponent,
        SnomedFooterComponent
    ],
    imports: [
        BrowserModule,
        HttpClientModule,
        FormsModule,
        ReactiveFormsModule,
        AppRoutingModule,
        ToastrModule.forRoot()
    ],
    providers: [
        PrincipleService,
        AuthenticationService,
        {
            provide: APP_INITIALIZER,
            useFactory: startupServiceFactory,
            deps: [PrincipleService],
            multi: true
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: HttpConfigInterceptor,
            multi: true
        }
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
