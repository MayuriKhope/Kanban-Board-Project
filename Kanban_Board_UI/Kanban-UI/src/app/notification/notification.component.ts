
import { Component, OnInit } from '@angular/core';
import { NotificationService } from '../services/notification.service';
import { MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.css']
})
export class NotificationComponent implements OnInit{
  constructor(public dialogRef: MatDialogRef<NotificationComponent> , private notification:NotificationService) {}
 
public notificationsList= null;
  ngOnInit(): void {
      this.notification.getNotification().subscribe((data:any)=>
      {
          this.notificationsList = data.body;
      });
  }
 
}
